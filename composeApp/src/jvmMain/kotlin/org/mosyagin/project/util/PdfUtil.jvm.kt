package org.mosyagin.project.util

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

actual fun extractTextFromPdf(uriString: String): String {
    return try {
        val file = File(uriString)
        val document = PDDocument.load(file)
        val stripper = PDFTextStripper()
        stripper.sortByPosition = true
        val text = stripper.getText(document)
        document.close()
        text
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}
