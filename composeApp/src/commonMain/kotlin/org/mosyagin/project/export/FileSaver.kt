package org.mosyagin.project.export

/**
 * Интерфейс для сохранения файлов на диск.
 * Реализуется отдельно для каждой платформы.
 */
interface FileSaver {
    /**
     * Запрашивает у пользователя место сохранения и записывает данные в файл.
     * @param fileName Предлагаемое имя файла.
     * @param mimeType MIME-тип файла (например, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").
     * @param bytes Содержимое файла.
     */
    fun saveFile(fileName: String, mimeType: String, bytes: ByteArray)
}
