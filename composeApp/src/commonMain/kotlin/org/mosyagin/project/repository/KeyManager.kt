package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.crypto.CryptoManager
import org.mosyagin.project.crypto.KeyVault
import org.mosyagin.project.logSync
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class KeyManager(
    private val queries: DatabaseQueries,
    private val cryptoManager: CryptoManager,
    private val keyVault: KeyVault,
    private val authRepository: AuthRepository,
    private val activityRepository: ActivityRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        authRepository.isEncryptionReady
            .filter { it }
            .onEach {
                logSync("KeyManager: Encryption ready. Attempting to unwrap all available project keys.")
                unwrapAvailableKeys()
            }
            .launchIn(scope)

        queries.getWrappedKeys()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .onEach { members ->
                val user = authRepository.getCurrentUserSync() ?: return@onEach
                val privateKey = keyVault.loadPrivateKey(user.id) ?: return@onEach
                
                members.forEach { member ->
                    if (member.wrapped_master_key != null && keyVault.loadMasterKey(member.project_id) == null) {
                        try {
                            val unwrapped = cryptoManager.unwrapKey(
                                Base64.decode(member.wrapped_master_key),
                                privateKey
                            )
                            keyVault.saveMasterKey(member.project_id, unwrapped)
                            logSync("KeyManager: Reactive unwrap for project ${member.project_id}")
                            decryptProjectData(member.project_id)
                        } catch (e: Exception) { }
                    }
                }
            }
            .launchIn(scope)
    }

    suspend fun unwrapAvailableKeys() {
        val user = authRepository.getCurrentUserSync() ?: return
        val privateKey = keyVault.loadPrivateKey(user.id) ?: return

        val membersWithKeys = queries.getWrappedKeys().executeAsList()
        membersWithKeys.forEach { member ->
            if (member.wrapped_master_key != null && keyVault.loadMasterKey(member.project_id) == null) {
                try {
                    val unwrapped = cryptoManager.unwrapKey(
                        Base64.decode(member.wrapped_master_key),
                        privateKey
                    )
                    keyVault.saveMasterKey(member.project_id, unwrapped)
                    decryptProjectData(member.project_id)
                } catch (e: Exception) { }
            } else if (keyVault.loadMasterKey(member.project_id) != null) {
                decryptProjectData(member.project_id)
            }
        }
    }

    suspend fun decryptProjectData(projectId: String) {
        val key = keyVault.loadMasterKey(projectId) ?: return
        
        logSync("KeyManager: Decrypting data for project $projectId")

        withContext(Dispatchers.IO) {
            // 1. Сцены
            queries.getUndecryptedScenes().executeAsList()
                .filter { it.project_id == projectId }
                .forEach { scene ->
                    val dec = if (scene.notes != null) cryptoManager.decryptText(scene.notes, key) else null
                    if (dec != "Ошибка расшифровки") {
                        queries.updateSceneNotesAfterDecryption(dec, scene.id)
                    }
                }

            // 2. Версии
            queries.getUndecryptedVersions().executeAsList()
                .forEach { version ->
                    val scene = queries.getSceneUserDataById(version.sceneUserDataId).executeAsOneOrNull()
                    if (scene?.project_id == projectId) {
                        val dec = cryptoManager.decryptText(version.content, key)
                        if (dec != "Ошибка расшифровки") {
                            queries.updateVersionContentAfterDecryption(dec, version.id)
                        }
                    }
                }

            // 3. Реквизит
            queries.getUndecryptedProps().executeAsList()
                .forEach { prop ->
                    val scene = queries.getSceneUserDataById(prop.sceneUserDataId).executeAsOneOrNull()
                    if (scene?.project_id == projectId) {
                        val dec = if (prop.note != null) cryptoManager.decryptText(prop.note, key) else null
                        if (dec != "Ошибка расшифровки") {
                            queries.updatePropNoteAfterDecryption(dec, prop.id)
                        }
                    }
                }

            // 4. ЖУРНАЛ АКТИВНОСТИ (Новое)
            activityRepository.decryptActivities(projectId)
        }
    }
}
