package com.github.badoualy.mobile.annotator

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.nio.PngWriter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Rectangle
import java.io.File

class ImageAnnotator : Annotator {

    private val WIDTH: Float = PDRectangle.A4.width

    override fun generatePdfDocument(file: File, files: List<File>) {
        // Generate pdf file for each flow with one page per step
        PDDocument().use { document ->
            files.forEach { it.addToPdfDocument(document) }
            document.save(file)
        }
    }

    override fun generateAnnotatedFile(pageContent: PageContent, screenshotFile: File, dir: File): File {
        return File(dir, "annotated_${screenshotFile.nameWithoutExtension}.png").also { file ->
            ImmutableImage.loader().fromFile(screenshotFile)
                .annotate(pageContent)
                .output(PngWriter.MaxCompression, file)
        }
    }

    private fun ImmutableImage.annotate(pageContent: PageContent): ImmutableImage {
        val bannedElements = listOfNotNull(pageContent.id, pageContent.scrollableElement)

        // Get header size and resize to add space at top
        val headerSize = awt().createGraphics().run {
            font = font.deriveFont(PAGE_HEADER_TEXT_SIZE)
            val value =
                (fontMetrics.height + fontMetrics.descent) * pageContent.headers.size + PAGE_HEADER_PADDING_VERTICAL
            dispose()
            value
        }

        return resizeTo(
            width + PAGE_HEADER_PADDING_HORIZONTAL * 2,
            height + headerSize,
            Position.BottomCenter
        ).apply {
            val bounds = Rectangle(width, height)
            awt().createGraphics().apply {
                // Draw header
                drawPageHeaders(pageContent, bounds)

                // Draw content
                translate(PAGE_HEADER_PADDING_HORIZONTAL, headerSize)
                pageContent.elements
                    .filter { it.id !in bannedElements }
                    .forEach(::drawElement)
            }.dispose()
        }
    }

    private fun File.addToPdfDocument(document: PDDocument) {
        val image = PDImageXObject.createFromFileByExtension(this, document)
        val pageRectangle = PDRectangle(WIDTH, image.height * WIDTH / image.width)
        val page = PDPage(pageRectangle).also { document.addPage(it) }
        PDPageContentStream(document, page).use { stream ->
            stream.drawImage(image, 0f, 0f, pageRectangle.width, pageRectangle.height)
        }
    }
}
