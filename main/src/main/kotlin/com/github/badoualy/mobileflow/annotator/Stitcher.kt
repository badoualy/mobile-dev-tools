package com.github.badoualy.mobileflow.annotator

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.pixels.Pixel
import java.io.File
import kotlin.system.measureTimeMillis

fun main() {
    val file1 = File("./main/resources/test01.png")
    val file2 = File("./main/resources/test02.png")
    val file3 = File("./main/resources/test03.png")
    val dst = File("./main/resources/result.png").apply { if (exists()) delete() }

    val scrollableViewTop = 56 * 5
    val scrollableViewBottom = 2870
    val duration = measureTimeMillis {
        listOf(file1, file2, file3)
            .getStitchedImage(startY = scrollableViewTop, endY = scrollableViewBottom)
            .output(PngWriter.MaxCompression, dst)
    }
    println("Stitched in $duration")
}

fun List<File>.getStitchedImage(startY: Int = 0, endY: Int = Integer.MAX_VALUE): ImmutableImage {
    val chunks = map { ImmutableImage.loader().fromFile(it) }.map { Chunk(it) }

    // Evaluate chunk bounds
    chunks.reduce { previousChunk, chunk ->
        check(previousChunk.image.width == chunk.image.width) { "Images must have the same width" }
        val result = checkNotNull(
            findFirstRowMatch(
                img1 = previousChunk.image,
                img2 = chunk.image,
                dropFirst = startY.coerceAtMost(chunk.image.height),
                dropLast = (chunk.image.height - endY).coerceAtLeast(0)
            )
        )

        previousChunk.bounds.bottom = result.first
        chunk.bounds.top = result.second
        chunk
    }

    // Build image
    val firstChunk = chunks.first()
    return ImmutableImage.create(firstChunk.image.width, chunks.sumBy { it.height })
        .apply {
            awt().createGraphics().apply {
                chunks.fold(0) { y, chunk ->
                    drawImage(
                        chunk.image.awt(),

                        0, y, width, y + chunk.height,
                        0, chunk.bounds.top, width, chunk.bounds.bottom,

                        null
                    )
                    y + chunk.height
                }
            }.dispose()
        }
}

private fun findFirstRowMatch(
    img1: ImmutableImage,
    img2: ImmutableImage,
    dropFirst: Int = 0,
    dropLast: Int = 0
): Pair<Int, Int>? {
    // Take first non empty line and look for it in img1
    val img2FirstRow = img2.rows().drop(dropFirst).firstOrNull { it.distinctBy(Pixel::argb).size > 1 } ?: return null

    val match = img1.rows().dropLast(dropLast).reversed().firstOrNull { it.isIdentical(img2FirstRow) }
    return if (match != null) {
        match[0].y to img2FirstRow[0].y
    } else {
        null
    }
}

private fun Array<out Pixel>.isIdentical(a: Array<out Pixel>): Boolean = all { it.argb == a[it.x].argb }

private data class Chunk(
    val image: ImmutableImage,
    val bounds: Bounds = Bounds(left = 0, top = 0, right = image.width, bottom = image.height)
) {
    val height get() = bounds.bottom - bounds.top
}

private data class Bounds(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0)
