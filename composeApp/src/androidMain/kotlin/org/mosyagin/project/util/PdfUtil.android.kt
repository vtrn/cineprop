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
        println("PDF_TRACE: --- START EXTRACTION ---")
        val uri = Uri.parse(uriString)
        val inputStream = appContext.contentResolver.openInputStream(uri) ?: return ""
        
        PDDocument.load(inputStream).use { document ->
            val stripper = ScriptPdfStripper()
            stripper.sortByPosition = true
            val text = stripper.getText(document)
            println("PDF_TRACE: --- FINISH EXTRACTION. Total length: ${text.length} ---")
            text
        }
    } catch (e: Exception) {
        println("PDF_TRACE: FATAL ERROR: ${e.message}")
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
        println("PDF_TRACE: [New Page Started]")
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
            val y = firstChar.yDirAdj
            val fontSize = firstChar.fontSizeInPt
            val fontName = firstChar.font?.name ?: "Unknown"
            
            // Расчет отступов для базы данных
            val spaceCount = ((x - 70) / 7.5).toInt().coerceIn(0, 60)
            
            // ПОДРОБНЫЙ ДЕБАГ В КОНСОЛЬ
            println("PDF_TRACE: LINE: X=${"%.1f".format(x)}, Y=${"%.1f".format(y)}, Size=${"%.1f".format(fontSize)}, Font='$fontName', Spaces=$spaceCount, Text='${cleanText.take(25)}...'")
            
            // Вставляем маркер жирного шрифта
            val isBold = fontName.contains("Bold", ignoreCase = true)
            if (isBold) {
                output.write("[B]")
            }

            if (spaceCount > 0) {
                output.write(" ".repeat(spaceCount))
            }
            isStartOfLine = false
        }

        var lastX = -1f
        for (pos in textPositions) {
            if (lastX > 0 && (pos.xDirAdj - lastX) > 13f) {
                output.write(" ")
            }
            output.write(pos.unicode)
            lastX = pos.xDirAdj + pos.widthDirAdj
        }
    }
}
