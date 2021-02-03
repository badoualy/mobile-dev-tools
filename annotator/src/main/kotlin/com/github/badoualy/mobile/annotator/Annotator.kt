package com.github.badoualy.mobile.annotator

import com.github.badoualy.mobile.stitcher.getStitchedImage
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.nio.PngWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.awt.Rectangle
import java.io.File
import kotlin.system.measureTimeMillis

private const val RESULT_DIR = "annotated"

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

fun main(args: Array<String>) {
    val dir = if (args.isEmpty()) {
        println("Missing args, using resources")
        File("./src/main/resources/")
    } else {
        File(args.first()).also {
            check(it.exists() && it.isDirectory) { "Supplied path doesn't exist or is not a dir: ${it.absolutePath}" }
        }
    }
    println("Looking in ${dir.absolutePath}")

    val duration = measureTimeMillis {
        dir.listFiles { file: File -> file.isDirectory }
            .orEmpty().toList()
            .parallelStream()
            .forEach(::generateFlowAnnotatedScreenshots)
    }

    println("Done in $duration")
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

    flow.steps
        .groupBy { it.uuid }.values
        .flatMap { pageContentList ->
            if (pageContentList.size > 1) {
                // Has multiple screenshots with same UUID, try to stitch
                try {
                    return@flatMap listOf(pageContentList.getStitchedPageContent(flowDir))
                } catch (e: Exception) {
                    println("Failed to stitch ${e.message}")
                }
            }

            // Default behavior in case of failure or single item
            pageContentList
        }
        .forEach { pageContent ->
            println("${pageContent.id}, ${pageContent.uuid}, ${pageContent.file}")
            val screenshotFile = File(flowDir, pageContent.file)
            val screenshotImage = ImmutableImage.loader().fromFile(screenshotFile)

            val annotatedFile = File(annotatedDir, "annotated_${screenshotFile.name}")
            screenshotImage.annotate(pageContent)
                .output(PngWriter.MaxCompression, annotatedFile)
        }

    println("\n")
}

private fun List<PageContent>.getStitchedPageContent(flowDir: File): PageContent {
    val uuid = first().uuid
    println("Attempting to stitch $uuid: ${joinToString { it.file }}")

    // Get scrollable element info
    val scrollableElement = mapNotNull {
        it.elements.firstOrNull { el -> el.id == it.scrollableElement }
    }.firstOrNull()
    check(all { it.scrollableElement.orEmpty().isNotBlank() }) { "Missing scrollableElement on some steps" }
    checkNotNull(scrollableElement) { "scrollableElement ref not found in elements" }

    // Stitch
    val files = map { File(flowDir, it.file) }
    val stitchedImage = files.getStitchedImage(
        startY = scrollableElement.y,
        endY = scrollableElement.run { y + height },
        threshold = 50
    )

    // Write stitched image to a tmp file for debug and to put the path in returned PageContent
    val stitchedFile = File(flowDir, "$uuid.png")
    stitchedImage.image.output(PngWriter.MaxCompression, stitchedFile)

    // Merge elements
    check(stitchedImage.chunks.size == size) { "Chunks size (${stitchedImage.chunks.size} doesn't match original list size ($size)" }
    var currentY = 0
    val elements = stitchedImage.chunks.flatMapIndexed { i, chunk ->
        get(i).elements
            // Keep only element inside chunked rectangle
            .filter { chunk.rectangle.contains(it.rectangle) }
            .map {
                it.copy(
                    id = it.id,
                    y = currentY + it.y - chunk.region.top
                )
            }
            .also { currentY += chunk.height }
    }

    return first().copy(
        file = stitchedFile.name,
        id = uuid,
        elements = elements
    )
}

private fun ImmutableImage.annotate(pageContent: PageContent): ImmutableImage {
    val bannedElements = listOfNotNull(pageContent.id, pageContent.scrollableElement)

    // Get header size and resize to add space at top
    val headerSize = awt().createGraphics().run {
        font = font.deriveFont(PAGE_HEADER_TEXT_SIZE)
        val value = (fontMetrics.height + fontMetrics.descent) * pageContent.headers.size + PAGE_HEADER_PADDING_VERTICAL
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

private val PageElement.rectangle get() = Rectangle(x, y, width, height)
