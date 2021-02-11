package com.github.badoualy.mobile.annotator

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File

object PdfHelper {

    private val WIDTH: Float = PDRectangle.A4.width

    fun generatePdf(file: File, imageList: List<File>) {
        PDDocument().use { document ->
            imageList.forEach { it.addToDocument(document) }
            document.save(file)
        }
    }

    private fun File.addToDocument(document: PDDocument) {
        val image = PDImageXObject.createFromFileByExtension(this, document)
        val pageRectangle = PDRectangle(WIDTH, image.height * WIDTH / image.width)
        val page = PDPage(pageRectangle).also { document.addPage(it) }
        PDPageContentStream(document, page).use { stream ->
            stream.drawImage(image, 0f, 0f, pageRectangle.width, pageRectangle.height)
        }
    }
}
