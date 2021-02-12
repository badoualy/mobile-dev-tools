package com.github.badoualy.mobile.annotator

import com.github.badoualy.mobile.stitcher.utils.pforEach
import com.github.badoualy.mobile.stitcher.utils.pmap
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.nio.PngWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Okio
import java.awt.Rectangle
import java.io.File
import kotlin.system.measureTimeMillis

private const val RESULT_DIR = "annotated"

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val flowAdapter = moshi.adapter(Flow::class.java)

internal data class Config(
    val input: File = File("."),
    val filter: File? = null,
    val threshold: Int = 50,
    val timeout: Long = 60_000L
)

/**
 * Usage: `./gradlew runAnnotator --args="<options>"`
 *
 * options:
 * - `--input <dir>` input directory
 * - `--filter <file>` filter file, each line is an element id that will be filtered out of the result
 * - `--threshold <value>` how many successive row should be identical to be considered a match (default: 50)
 * - `--timeout <value>` timeout before aborting merge
 */
fun main(args: Array<String>) {
    val config = getConfig(args)
    println("config $config")

    val inputDir = config.input
    println("Looking in ${inputDir.absolutePath}")
    val duration = measureTimeMillis {
        runBlocking(Dispatchers.Default) {
            inputDir.listFiles { file: File -> file.isDirectory }
                .orEmpty().toList()
                .pmap { flowDir -> flowDir to generateFlowAnnotatedScreenshots(flowDir, config) }
                .pforEach { (flowDir, flowScreenshots) ->
                    val pdfFile = File(flowDir, "flow.pdf")
                    PdfHelper.generatePdf(pdfFile, flowScreenshots)
                }
        }
    }

    println("Done in $duration")
}

private fun getConfig(args: Array<String>): Config {
    return args.toList().zipWithNext().fold(Config()) { config, (option, value) ->
        when (option) {
            "--input" -> {
                config.copy(
                    input = File(value).also {
                        check(it.exists() && it.isDirectory) { "Supplied path doesn't exist or is not a dir: ${it.absolutePath}" }
                    }
                )
            }
            "--filter" -> {
                config.copy(
                    input = File(value).also {
                        check(it.exists() && it.isFile) { "Supplied filter file doesn't exist or is not a file: ${it.absolutePath}" }
                    }
                )
            }
            "--threshold" -> config.copy(threshold = value.toInt())
            "--timeout" -> config.copy(timeout = value.toLong())
            else -> config
        }
    }
}

private suspend fun generateFlowAnnotatedScreenshots(flowDir: File, config: Config): List<File> {
    val jsonFile = flowDir.listFiles { file: File -> file.extension.toLowerCase() == "json" }?.firstOrNull()
    if (jsonFile == null) {
        println("Found no json in ${flowDir.name}, skipping")
        return emptyList()
    }

    val annotatedDir = File(flowDir, RESULT_DIR).apply {
        if (exists()) deleteRecursively()
        mkdir()
    }

    val flow = flowAdapter.fromJson(Okio.buffer(Okio.source(jsonFile))) ?: return emptyList()
    println("Starting flow ${flow.flowName}")

    return flow.steps
        .groupBy { it.uuid }.values
        .flatMap { pageContentList ->
            if (pageContentList.size > 1) {
                // Has multiple screenshots with same UUID, try to stitch
                try {
                    return@flatMap listOf(pageContentList.getStitchedPageContent(flowDir, config))
                } catch (e: Exception) {
                    println("Failed to stitch ${e.message}")
                }
            }

            // Default behavior in case of failure or single item
            pageContentList
        }
        .map { pageContent ->
            println("${pageContent.id}, ${pageContent.uuid}, ${pageContent.file}")
            val screenshotFile = File(flowDir, pageContent.file)
            val annotatedFile = File(annotatedDir, "annotated_${screenshotFile.name}")

            ImmutableImage.loader().fromFile(screenshotFile)
                .annotate(pageContent)
                .output(PngWriter.MaxCompression, annotatedFile)

            annotatedFile
        }
}

private fun ImmutableImage.annotate(pageContent: PageContent): ImmutableImage {
    val bannedElements = listOfNotNull(pageContent.id, pageContent.scrollableElement)

    // Get header size and resize to add space at top
    val headerSize = awt().createGraphics().run {
        font = font.deriveFont(PAGE_HEADER_TEXT_SIZE)
        val value = (fontMetrics.height + fontMetrics.descent) * pageContent.headers.size + PAGE_HEADER_PADDING_VERTICAL
        dispose()
        value
    }

    return resizeTo(
        width + PAGE_HEADER_PADDING_HORIZONTAL * 2,
        height + headerSize,
        Position.BottomCenter
    ).apply {
        val bounds = Rectangle(width, height)
        awt().createGraphics().apply {
            // Draw header
            drawPageHeaders(pageContent, bounds)

            // Draw content
            translate(PAGE_HEADER_PADDING_HORIZONTAL, headerSize)
            pageContent.elements
                .filter { it.id !in bannedElements }
                .forEach(::drawElement)
        }.dispose()
    }
}

