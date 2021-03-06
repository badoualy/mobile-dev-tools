package com.github.badoualy.mobile.annotator

data class Flow(
    val flowName: String,
    val steps: List<PageContent>
)

data class PageContent(
    val file: String,
    val id: String,
    val uuid: String,
    val deeplink: String,
    val fragmentName: String,
    val controllerName: String,
    val jsFileName: String,
    val scrollableElement: String? = null,
    val elements: List<PageElement>
) {
    val headers: List<String> get() = listOf(id, deeplink, fragmentName, controllerName, jsFileName)
}

data class PageElement(
    val id: String,
    val annotate: Boolean = true,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)
