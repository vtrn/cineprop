package org.mosyagin.project.util

import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import org.mosyagin.project.db.appContext
import java.io.IOException

actual fun extractTextFromPdf(uriString: String): String {
    return try {
        println("PDF_DEBUG: Starting extraction from $uriString")
        val uri = Uri.parse(uriString)
        val inputStream = appContext.contentResolver.openInputStream(uri) ?: return ""
        
        PDDocument.load(inputStream).use { document ->
            val stripper = ScriptPdfStripper()
            stripper.sortByPosition = true
            val text = stripper.getText(document)
            println("PDF_DEBUG: Extraction finished. Text length: ${text.length}")
            text
        }
    } catch (e: Exception) {
        println("PDF_DEBUG: Error during extraction: ${e.message}")
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
            
            val spaceCount = ((x - 70) / 8).toInt().coerceIn(0, 50)
            if (spaceCount > 0) {
                val indent = " ".repeat(spaceCount)
                output.write(indent)
            }
            // Выводим в лог информацию о начале строки, координате X и посчитанном отступе
            println("PDF_DEBUG: Line start at X=${"%.2f".format(x)}, indent spaces=$spaceCount, text sample='${text?.take(20)?.replace("\n", " ")}...'")
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
