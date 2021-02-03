package com.github.badoualy.mobile.stitcher

import com.sksamuel.scrimage.ImmutableImage
import java.awt.Rectangle

data class StitchedImage(val image: ImmutableImage, val chunks: List<Chunk>)

data class Chunk(
    val image: ImmutableImage,
    val region: Region = Region(left = 0, top = 0, right = image.width, bottom = image.height)
) {
    val height get() = region.run { bottom - top }
    val rectangle get() = region.run { Rectangle(left, top, right - left, bottom - top) }
}

data class Region(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0)
