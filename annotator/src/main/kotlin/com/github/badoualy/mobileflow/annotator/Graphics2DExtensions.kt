package com.github.badoualy.mobileflow.annotator

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle

val ELEMENT_BG_COLOR: Color = Color.WHITE
val ELEMENT_TEXT_COLOR: Color = Color.RED
const val ELEMENT_TEXT_SIZE = 32f
const val ELEMENT_RECTANGLE_PADDING = 8

const val PAGE_HEADER_PADDING_VERTICAL = 50
const val PAGE_HEADER_PADDING_HORIZONTAL = 200
val PAGE_HEADER_TEXT_COLOR: Color = Color.BLACK
const val PAGE_HEADER_TEXT_SIZE = 42f

fun Graphics2D.drawElement(element: PageElement) {
    font = font.deriveFont(ELEMENT_TEXT_SIZE)
    val textBounds = fontMetrics.getStringBounds(element.id, this)
    val textRect = Rectangle(element.x, element.y, textBounds.width.toInt(), textBounds.height.toInt())

    color = ELEMENT_BG_COLOR
    fillRect(textRect.x, textRect.y, textRect.width + ELEMENT_RECTANGLE_PADDING * 2, textRect.height)

    color = ELEMENT_TEXT_COLOR
    drawString(element.id, textRect.x + ELEMENT_RECTANGLE_PADDING, textRect.y + textRect.height - fontMetrics.descent)
}

fun Graphics2D.drawPageHeaders(pageContent: PageContent, imageBounds: Rectangle) {
    font = font.deriveFont(PAGE_HEADER_TEXT_SIZE)
    color = PAGE_HEADER_TEXT_COLOR
    drawLines(pageContent.headers, imageBounds = imageBounds)
}

fun Graphics2D.drawLines(list: List<String>, initialY: Int = 0, imageBounds: Rectangle) {
    list.forEachIndexed { i, text ->
        drawStringCenteredHorizontally(
            text = text,
            y = initialY + (i + 1) * (fontMetrics.height + fontMetrics.descent),
            imageBounds = imageBounds
        )
    }
}

fun Graphics2D.drawStringCenteredHorizontally(text: String, y: Int, imageBounds: Rectangle) {
    // deviceConfiguration.bounds.width doesn't work in console :shrug:

    val width = fontMetrics.stringWidth(text)
    drawString(
        text,
        (imageBounds.width - width) / 2,
        y
    )
}

