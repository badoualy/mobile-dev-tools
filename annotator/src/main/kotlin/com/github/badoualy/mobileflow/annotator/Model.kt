package com.github.badoualy.mobileflow.annotator

data class Flow(
    val flowName: String,
    val steps: List<PageContent>
)

data class PageContent(
    val files: List<String>,
    val id: String,
    val deeplink: String,
    val fragmentName: String,
    val controllerName: String,
    val jsFileName: String,
    val elements: List<PageElement>
) {
    val headers: List<String> get() = listOf(id, deeplink, fragmentName, controllerName, jsFileName)
}

data class PageElement(
    val id: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)
