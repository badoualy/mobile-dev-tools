package com.github.badoualy.mobile.annotator

import com.github.badoualy.mobile.stitcher.StitcherConfig
import com.github.badoualy.mobile.stitcher.getStitchedImage
import com.sksamuel.scrimage.nio.PngWriter
import java.awt.Rectangle
import java.io.File

internal suspend fun List<PageContent>.getStitchedPageContent(flowDir: File, config: StitcherConfig): PageContent {
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
        threshold = config.threshold,
        timeout = config.timeout
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

val PageElement.rectangle get() = Rectangle(x, y, width, height)

