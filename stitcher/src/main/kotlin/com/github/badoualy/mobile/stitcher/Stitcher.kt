package com.github.badoualy.mobile.stitcher

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.pixels.Pixel
import java.io.File

fun main(args: Array<String>) {
    check(args.isNotEmpty()) { "Missing input directory" }

    val inputDir = File(args[0])
    val resultFile = File(inputDir, "result.png").apply { if (exists()) delete() }

    println("Looking for files in ${inputDir.absolutePath}")
    val files = inputDir.listFiles { file: File ->
        file.extension.toLowerCase() in arrayOf("jpg", "png", "jpeg")
    }?.toList()?.sortedBy { it.nameWithoutExtension }
    println("Stitching files ${files.orEmpty().joinToString { it.name }}")

    files?.getStitchedImage(
        startY = args.getOrNull(1)?.toInt() ?: 0,
        endY = args.getOrNull(2)?.toInt() ?: 0,
        threshold = args.getOrNull(3)?.toInt() ?: 0
    )?.output(PngWriter.MaxCompression, resultFile)
}

fun List<File>.getStitchedImage(
    startY: Int = 0,
    endY: Int = Integer.MAX_VALUE,
    threshold: Int = 1
): ImmutableImage {
    val chunks = map { ImmutableImage.loader().fromFile(it) }.map { Chunk(it) }

    // Evaluate chunk bounds
    chunks.reduce { previousChunk, chunk ->
        check(previousChunk.image.width == chunk.image.width) { "Images must have the same width" }
        val result = checkNotNull(
            findFirstRowMatch(
                img1 = previousChunk.image,
                img2 = chunk.image,
                dropFirst = startY.coerceAtMost(chunk.image.height),
                dropLast = (chunk.image.height - endY).coerceAtLeast(0),
                threshold = threshold
            )
        )
        println("Match $result")

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
    dropFirst: Int,
    dropLast: Int,
    threshold: Int
): Pair<Int, Int>? {
    img2.rows().drop(dropFirst)
        .filter { it.distinctBy(Pixel::argb).size > 1 }
        .forEach { img2Row ->
            val match = img1.rows()
                .dropLast(dropLast) // Ignore what's outside of the scrolling view
                .reversed() // Start from bottom to find the match faster
                .filter { it[0].y != img2Row[0].y } // Ignore identical row, probably not in the scrolling view's bounds
                .firstOrNull { it.isIdentical(img2Row) }
            if (match != null && areRowsIdentical(img1, img2, match[0].y, img2Row[0].y, threshold = threshold)) {
                return match[0].y to img2Row[0].y
            }
        }

    return null
}

private fun areRowsIdentical(
    img1: ImmutableImage,
    img2: ImmutableImage,
    startRow1: Int,
    startRow2: Int,
    threshold: Int
): Boolean {
    return (0 until threshold).all { img1.row(startRow1 + it).isIdentical(img2.row(startRow2 + it)) }
}

private fun Array<out Pixel>.isIdentical(a: Array<out Pixel>): Boolean = all { it.argb == a[it.x].argb }

private data class Chunk(
    val image: ImmutableImage,
    val bounds: Bounds = Bounds(left = 0, top = 0, right = image.width, bottom = image.height)
) {
    val height get() = bounds.bottom - bounds.top
}

private data class Bounds(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0)
