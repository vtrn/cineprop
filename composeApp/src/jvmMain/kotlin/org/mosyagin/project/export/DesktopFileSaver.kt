package org.mosyagin.project.export

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Реализация сохранения файлов для платформы Desktop (JVM).
 * Использует стандартный JFileChooser.
 */
class DesktopFileSaver : FileSaver {
    override suspend fun saveFile(fileName: String, mimeType: String, bytes: ByteArray) {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Сохранить файл"
            selectedFile = File(fileName)
            
            // Настройка фильтра расширений на основе MIME-типа
            if (mimeType.contains("spreadsheetml")) {
                fileFilter = FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx")
            } else if (mimeType.contains("pdf")) {
                fileFilter = FileNameExtensionFilter("PDF Files (*.pdf)", "pdf")
            }
        }

        val userSelection = fileChooser.showSaveDialog(null)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            var fileToSave = fileChooser.selectedFile
            
            // Добавляем расширение, если пользователь забыл
            if (mimeType.contains("spreadsheetml") && !fileToSave.name.endsWith(".xlsx")) {
                fileToSave = File("${fileToSave.absolutePath}.xlsx")
            } else if (mimeType.contains("pdf") && !fileToSave.name.endsWith(".pdf")) {
                fileToSave = File("${fileToSave.absolutePath}.pdf")
            }

            try {
                fileToSave.writeBytes(bytes)
                println("Файл успешно сохранен: ${fileToSave.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
