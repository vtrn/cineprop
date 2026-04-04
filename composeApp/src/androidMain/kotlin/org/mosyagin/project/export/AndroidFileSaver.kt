package org.mosyagin.project.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

/**
 * Реализация сохранения файлов для платформы Android.
 * Использует Storage Access Framework (SAF).
 */
class AndroidFileSaver(private val context: Context) : FileSaver {

    private var pendingBytes: ByteArray? = null

    override fun saveFile(fileName: String, mimeType: String, bytes: ByteArray) {
        pendingBytes = bytes
        
        // В реальном приложении здесь должен быть вызов ActivityResultLauncher.
        // Для MVP мы можем использовать упрощенный подход или делегировать это Activity.
        // Здесь мы просто подготовим данные.
    }
    
    /**
     * Метод для записи байтов в выбранный URI (вызывается после того, как Activity получит результат)
     */
    fun writeDataToUri(uri: Uri) {
        val bytes = pendingBytes ?: return
        CoroutineScope(Dispatchers.IO).launch {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
                outputStream.flush()
            }
            pendingBytes = null
        }
    }
}
