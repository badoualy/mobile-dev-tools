package com.github.badoualy.mobileflow.annotator

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.nio.PngWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.awt.Rectangle
import java.io.File

private const val RESULT_DIR = "annotated"

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

fun main(args: Array<String>) {
    val dir = if (args.isEmpty()) {
        println("Missing args, using resources")
        File("./main/src/main/resources/")
    } else {
        File(args.first()).also {
            check(it.exists() && it.isDirectory) { "Supplied path doesn't exist or is not a dir: ${it.absolutePath}" }
        }
    }

    dir.listFiles { file: File -> file.isDirectory }
        .orEmpty()
        .forEach(::generateFlowAnnotatedScreenshots)

    println("Done")
}

private fun generateFlowAnnotatedScreenshots(flowDir: File) {
    val jsonFile = flowDir.listFiles { file: File -> file.extension.toLowerCase() == "json" }?.firstOrNull()
    if (jsonFile == null) {
        println("Found no json in ${flowDir.name}, skipping")
        return
    }

    val annotatedDir = File(flowDir, RESULT_DIR).apply { mkdir() }

    val flow = moshi.adapter(Flow::class.java).fromJson(Okio.buffer(Okio.source(jsonFile))) ?: return
    println("Starting flow ${flow.flowName}")
    flow.steps.forEach {
        val screenshotFile = File(flowDir, it.file)
        val annotatedFile = File(annotatedDir, "annotated_${screenshotFile.name}")
        generateAnnotatedScreenshot(flowDir, it)
            .output(PngWriter.MaxCompression, annotatedFile)
        println("Annotated ${it.file}")
    }

    println("\n")
}

private fun generateAnnotatedScreenshot(file: File, pageContent: PageContent): ImmutableImage {
    var headerSize: Int
    return ImmutableImage.loader().fromFile(file)
        .run {
            // Get header size and resize to add space at top
            headerSize = awt().createGraphics().run {
                font = font.deriveFont(PAGE_HEADER_TEXT_SIZE)
                (fontMetrics.height + fontMetrics.descent) * pageContent.headers.size
            } + PAGE_HEADER_PADDING_VERTICAL

            resizeTo(
                width + PAGE_HEADER_PADDING_HORIZONTAL * 2,
                height + headerSize,
                Position.BottomCenter
            )
        }
        .apply {
            val bounds = Rectangle(width, height)
            awt().createGraphics().apply {
                // Draw header
                drawPageHeaders(pageContent, bounds)

                // Draw content
                translate(PAGE_HEADER_PADDING_HORIZONTAL, headerSize)
                pageContent.elements.forEach(::drawElement)
            }
        }
}
