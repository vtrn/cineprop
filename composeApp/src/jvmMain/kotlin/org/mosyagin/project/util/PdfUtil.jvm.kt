package org.mosyagin.project.util

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.File
import java.io.IOException

actual fun extractTextFromPdf(uriString: String): String {
    return try {
        val file = File(uriString)
        PDDocument.load(file).use { document ->
            val stripper = ScriptPdfStripper()
            stripper.sortByPosition = true
            stripper.getText(document)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

private class ScriptPdfStripper : PDFTextStripper() {
    private var isStartOfLine = true

    @Throws(IOException::class)
    override fun writeLineSeparator() {
        super.writeLineSeparator()
        isStartOfLine = true
    }

    @Throws(IOException::class)
    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        if (textPositions.isNullOrEmpty()) return

        if (isStartOfLine) {
            val firstChar = textPositions[0]
            val x = firstChar.xDirAdj
            
            // Расчет пробелов: вычитаем базовое поле (70) и делим на ширину Courier-символа (8)
            val spaceCount = ((x - 70) / 8).toInt().coerceIn(0, 50)
            if (spaceCount > 0) {
                output.write(" ".repeat(spaceCount))
            }
            isStartOfLine = false
        }

        var lastX = -1f
        for (pos in textPositions) {
            if (lastX > 0 && (pos.xDirAdj - lastX) > 15f) {
                output.write(" ")
            }
            output.write(pos.unicode)
            lastX = pos.xDirAdj + pos.widthDirAdj
        }
    }
}
