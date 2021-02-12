package com.github.badoualy.mobile.annotator

import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import java.io.File

class PdfAnnotator : Annotator {

    override fun generatePdfDocument(file: File, files: List<File>) {
        PDFMergerUtility().apply {
            destinationFileName = file.absolutePath
            files.forEach(::addSource)
        }.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
    }

    override fun generateAnnotatedFile(pageContent: PageContent, screenshotFile: File, dir: File): File {
        TODO("PDF annotation is not implemented yet")
    }
}
