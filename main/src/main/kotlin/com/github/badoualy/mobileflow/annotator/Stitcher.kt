package com.github.badoualy.mobileflow.annotator

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
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
            .stitch(startY = scrollableViewTop, endY = scrollableViewBottom)
            .output(PngWriter.MaxCompression, dst)
    }
    println("Stitched in $duration")
}

private fun List<File>.stitch(startY: Int, endY: Int): ImmutableImage {
    return map { ImmutableImage.loader().fromFile(it) }
        .reduce { acc, image ->
            check(acc.width == image.width) { "Images must have the same width" }
            val result = checkNotNull(findFirstRowMatch(acc, image, dropFirst = startY, dropLast = image.height - endY))

            val bottomChunkSize = image.height - result.second
            acc.resizeTo(acc.width, result.first + bottomChunkSize, Position.TopLeft)
                .overlay(image.takeBottom(bottomChunkSize), Position.BottomLeft)
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
        println("Found")
        match[0].y to img2FirstRow[0].y
    } else {
        null
    }
}

private fun Array<out Pixel>.isIdentical(a: Array<out Pixel>): Boolean = all { it.argb == a[it.x].argb }
