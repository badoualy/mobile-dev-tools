@file:Suppress("BlockingMethodInNonBlockingContext")

package com.github.badoualy.mobile.annotator

import com.github.badoualy.mobile.stitcher.utils.pforEach
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Okio
import java.io.File
import kotlin.system.measureTimeMillis

private const val ANNOTATED_DIR = "annotated"

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val flowAdapter = moshi.adapter(Flow::class.java)

interface Annotator {
    fun generatePdfDocument(file: File, files: List<File>)
    fun generateAnnotatedFile(pageContent: PageContent, screenshotFile: File, dir: File): File
}

/**
 * Usage: `./gradlew runAnnotator --args="<options>"`
 *
 * options:
 * - `--input <dir>` input directory
 * - `--filter <file>` filter file, each line is an element id that will be filtered out of the result
 * - `--annotatePdf true|false` if true, the annotations will be written as text on the pdf instead of on the image directly (default: false)
 * - `--threshold <value>` how many successive row should be identical to be considered a match (default: 50)
 * - `--timeout <value>` timeout before aborting merge
 */
fun main(args: Array<String>) {
    val config = AnnotatorConfig.parseArguments(args)
    println("config $config")

    val inputDir = config.input
    println("Looking in ${inputDir.absolutePath}")

    val duration = measureTimeMillis {
        runBlocking(Dispatchers.Default) {
            inputDir.listFiles { file: File -> file.isDirectory }
                .orEmpty().toList()
                .pforEach { flowDir -> generateFlowDocument(flowDir = flowDir, config = config) }
        }
    }

    println("Done in $duration")
}

private suspend fun generateFlowDocument(
    flowDir: File,
    config: AnnotatorConfig
): File? {
    val jsonFile = flowDir.listFiles { file: File -> file.extension.toLowerCase() == "json" }?.firstOrNull()
    if (jsonFile == null) {
        println("Found no json in ${flowDir.name}, skipping")
        return null
    }

    val annotatedDir = File(flowDir, ANNOTATED_DIR).apply {
        if (exists()) deleteRecursively()
        mkdir()
    }

    val filters = config.filter?.readLines()?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }.orEmpty()
    println("Filters ${filters.joinToString()}")

    val flow = flowAdapter.fromJson(Okio.buffer(Okio.source(jsonFile))) ?: return null
    println("Starting flow ${flow.flowName}")

    val annotator: Annotator = if (config.annotatePdf) PdfAnnotator() else ImageAnnotator()
    println("""Using ${if (config.annotatePdf) "Pdf" else "Image"}Annotator""")

    val annotatedFiles = flow.stitchSteps(flowDir, config.stitcherConfig).steps.map { step ->
        println("${step.id}, ${step.uuid}, ${step.file}")
        annotator.generateAnnotatedFile(
            pageContent = step.run {
                // Keep elements not in filter and in selector if inSelectorsOnly is enabled
                copy(elements = elements.filter { it.id !in filters && (it.inSelectors || !config.inSelectorsOnly) })
            },
            screenshotFile = File(flowDir, step.file),
            dir = annotatedDir
        )
    }

    return File(flowDir, "flow.pdf").also { annotator.generatePdfDocument(it, annotatedFiles) }
}
