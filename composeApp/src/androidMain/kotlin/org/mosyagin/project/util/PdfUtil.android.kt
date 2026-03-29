package org.mosyagin.project.util

import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.mosyagin.project.db.appContext

actual fun extractTextFromPdf(uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        val inputStream = appContext.contentResolver.openInputStream(uri)
        val document = PDDocument.load(inputStream)
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
