package com.github.badoualy.mobile.annotator

import com.github.badoualy.mobile.stitcher.StitcherConfig
import java.io.File

data class AnnotatorConfig(
    val input: File = File("."),
    val filter: File? = null,
    val annotatePdf: Boolean = false,
    val useAnnotateProperty: Boolean = false,
    val stitcherConfig: StitcherConfig = StitcherConfig()
) {

    companion object {
        fun parseArguments(args: Array<String>): AnnotatorConfig {
            val stitcherConfig = StitcherConfig.parseArguments(args)
            return args.toList().zipWithNext()
                .fold(AnnotatorConfig(stitcherConfig = stitcherConfig)) { config, (option, value) ->
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
                                filter = File(value).also {
                                    check(it.exists() && it.isFile) { "Supplied filter file doesn't exist or is not a file: ${it.absolutePath}" }
                                }
                            )
                        }
                        "--annotatePdf" -> config.copy(annotatePdf = value.toBoolean())
                        "--useAnnotateProperty" -> config.copy(useAnnotateProperty = value.toBoolean())
                        else -> config
                    }
                }
        }
    }
}
