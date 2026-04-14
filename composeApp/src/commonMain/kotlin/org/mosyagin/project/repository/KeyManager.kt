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
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // 1. Следим за готовностью приватного ключа пользователя
        authRepository.isEncryptionReady
            .filter { it }
            .onEach {
                logSync("KeyManager: Encryption ready. Attempting to unwrap all available project keys.")
                unwrapAvailableKeys()
            }
            .launchIn(scope)

        // 2. Реактивно следим за таблицей ProjectMember. 
        // Если прилетел новый wrapped_master_key по Realtime — пытаемся его развернуть.
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
                        } catch (e: Exception) {
                            // Игнорируем ошибки (возможно ключ не для нас)
                        }
                    }
                }
            }
            .launchIn(scope)
    }

    /**
     * Пытается расшифровать все завернутые мастер-ключи проектов
     */
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
                    logSync("KeyManager: Unwrapped key for ${member.project_id}")
                    decryptProjectData(member.project_id)
                } catch (e: Exception) {
                    logSync("KeyManager: Unwrapping failed for ${member.project_id}")
                }
            } else if (keyVault.loadMasterKey(member.project_id) != null) {
                // Если ключ уже есть, проверяем нерасшифрованные данные
                decryptProjectData(member.project_id)
            }
        }
    }

    /**
     * Фоновая расшифровка данных проекта
     */
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
        }
    }
}
