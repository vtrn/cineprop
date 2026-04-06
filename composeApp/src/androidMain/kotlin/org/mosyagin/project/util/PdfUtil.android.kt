package org.mosyagin.project.util

import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import org.mosyagin.project.db.appContext
import java.io.IOException

actual fun extractTextFromPdf(uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        val inputStream = appContext.contentResolver.openInputStream(uri) ?: return ""
        
        PDDocument.load(inputStream).use { document ->
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
    private val pageNumberRegex = Regex("""^\s*(\d+|стр\.\s*\d+|page\s*\d+)\s*$""", RegexOption.IGNORE_CASE)

    @Throws(IOException::class)
    override fun startPage(page: PDPage?) {
        super.startPage(page)
        isStartOfLine = true
    }

    @Throws(IOException::class)
    override fun writeLineSeparator() {
        super.writeLineSeparator()
        isStartOfLine = true
    }

    @Throws(IOException::class)
    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        if (textPositions.isNullOrEmpty()) return
        
        val cleanText = text?.trim() ?: ""
        if (cleanText.isEmpty() || cleanText.matches(pageNumberRegex)) return

        if (isStartOfLine) {
            val firstChar = textPositions[0]
            val x = firstChar.xDirAdj
            
            // Синхронизированный расчет отступов (база 70, шаг 8)
            val spaceCount = ((x - 70) / 8.0).toInt().coerceIn(0, 50)
            
            if (spaceCount > 0) {
                output.write(" ".repeat(spaceCount))
            }
            isStartOfLine = false
        }

        var lastX = -1f
        for (pos in textPositions) {
            // Синхронизированный порог пробела 14f
            if (lastX > 0 && (pos.xDirAdj - lastX) > 14f) {
                output.write(" ")
            }
            output.write(pos.unicode)
            lastX = pos.xDirAdj + pos.widthDirAdj
        }
    }
}
