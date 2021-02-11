package com.github.badoualy.mobile.stitcher

import com.github.badoualy.mobile.stitcher.utils.hasAtLeastDistinctBy
import com.github.badoualy.mobile.stitcher.utils.pmap
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File

var DEBUG = false
private val DEBUG_COLORS = arrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.CYAN)

fun main(args: Array<String>) {
    check(args.isNotEmpty()) { "Missing input directory" }

    val inputDir = File(args[0])
    val resultFile = File(inputDir, "result.png").apply { if (exists()) delete() }

    println("Looking for files in ${inputDir.absolutePath}")
    val files = inputDir.listFiles { file: File ->
        file.extension.toLowerCase() in arrayOf("jpg", "png", "jpeg")
    }?.toList()?.sortedBy { it.nameWithoutExtension }
    println("Stitching files ${files.orEmpty().joinToString { it.name }}")

    val timeout = args.getOrNull(4)?.toLong() ?: 60_000L
    runBlocking(Dispatchers.Default) {
        files?.getStitchedImage(
            startY = args.getOrNull(1)?.toInt() ?: 0,
            endY = args.getOrNull(2)?.toInt() ?: 0,
            threshold = args.getOrNull(3)?.toInt() ?: 0,
            timeout = timeout
        )?.image?.output(PngWriter.MaxCompression, resultFile)
    }
}

suspend fun List<File>.getStitchedImage(
    startY: Int = 0,
    endY: Int = Integer.MAX_VALUE,
    threshold: Int = 1,
    timeout: Long = 2 * 60 * 1000L
): StitchedImage {
    return withTimeout(timeout) {
        map { ImmutableImage.loader().fromFile(it) }
            .zipWithNext()
            .pmap { (img1, img2) ->
                check(img1.width == img2.width) { "Images must have the same width" }
                findFirstRowMatch(
                    img1 = img1,
                    img2 = img2,
                    dropFirst = startY.coerceAtMost(img2.height),
                    dropLast = (img1.height - endY).coerceAtLeast(0),
                    threshold = threshold
                ).let { checkNotNull(it) }
            }
            .toList()
            .run {
                // Build ChunkList with region of each picture
                runningFold(Chunk(first().img1)) { previousChunk, result ->
                    previousChunk.region.bottom = result.y1
                    Chunk(result.img2).apply { region.top = result.y2 }
                }
            }
            .buildStitchedImage()
    }
}

private fun List<Chunk>.buildStitchedImage(): StitchedImage {
    val stitchedImage = ImmutableImage.create(first().image.width, sumBy { it.height })
        .apply {
            awt().createGraphics().apply {
                fold(0) { y, chunk ->
                    drawImage(
                        chunk.image.awt(),

                        0, y, width, y + chunk.height,
                        0, chunk.region.top, width, chunk.region.bottom,

                        null
                    )

                    y + chunk.height
                }

                if (DEBUG) {
                    foldIndexed(0) { i, y, chunk ->
                        color = DEBUG_COLORS[i % DEBUG_COLORS.size]
                        drawRect(0, y, width, chunk.height - 1)
                        y + chunk.height
                    }
                }
            }.dispose()
        }

    return StitchedImage(stitchedImage, this)
}

private fun findFirstRowMatch(
    img1: ImmutableImage,
    img2: ImmutableImage,
    dropFirst: Int,
    dropLast: Int,
    threshold: Int
): MatchResult? {
    check(img1.width == img2.width) { "Images must have the same width" }

    val img1RowIndices = img1.rowIndices()
        .drop(dropFirst).dropLast(dropLast) // Ignore what's outside of the scrolling view
        .reversed() // Start from bottom to find the match faster
    val columnIndices = img1.columnIndices()

    img2.rowIndices()
        .drop(dropFirst).dropLast(dropLast) // Ignore what's outside of the scrolling view
        .filter { y ->
            // Only check lines that have at least 2 different pixel value
            columnIndices.hasAtLeastDistinctBy(2) { x -> img2.awt().getRGB(x, y) }
        }
        .forEach { y2 ->
            val y1Match = img1RowIndices
                // .filter { y1 -> y1 != y2 } // Ignore identical row
                .firstOrNull { y1 ->
                    areRowsIdentical(img1.awt(), img2.awt(), y1, y2, columnIndices, threshold = threshold)
                }
            if (y1Match != null) {
                return MatchResult(img1, img2, y1Match, y2)
            }
        }

    return null
}

private fun areRowsIdentical(
    img1: BufferedImage,
    img2: BufferedImage,
    y1: Int,
    y2: Int,
    columnIndices: List<Int> = img1.columnIndices(),
    threshold: Int = 1
): Boolean {
    check(img1.width == img2.width) { "Images must have the same width" }
    return (0 until threshold).all { yDelta ->
        // If one is out of bounds, but not the other, consider it's not a match
        if (y1 + yDelta >= img1.height != y2 + yDelta >= img2.height) return false
        return columnIndices.all { x ->
            img1.getRGB(x, y1 + yDelta) == img2.getRGB(x, y2 + yDelta)
        }
    }
}

private data class MatchResult(val img1: ImmutableImage, val img2: ImmutableImage, val y1: Int, val y2: Int)

private fun ImmutableImage.rowIndices() = (0 until height).toList()
private fun ImmutableImage.columnIndices() = (0 until width).toList()
private fun BufferedImage.rowIndices() = (0 until height).toList()
private fun BufferedImage.columnIndices() = (0 until width).toList()
