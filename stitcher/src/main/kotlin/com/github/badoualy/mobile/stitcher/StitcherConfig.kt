package com.github.badoualy.mobile.stitcher

import java.io.File

data class StitcherConfig(
    val input: File = File("."),
    val bounds: Pair<Int, Int> = 0 to Integer.MAX_VALUE,
    val threshold: Int = 50,
    val timeout: Long = 60_000L,
    val debug: Boolean = false
) {

    companion object {
        fun parseArguments(args: Array<String>): StitcherConfig {
            return args.toList().zipWithNext().fold(StitcherConfig()) { config, (option, value) ->
                when (option) {
                    "--input" -> {
                        config.copy(
                            input = File(value).also {
                                check(it.exists() && it.isDirectory) { "Supplied path doesn't exist or is not a dir: ${it.absolutePath}" }
                            }
                        )
                    }
                    "--bounds" -> config.copy(bounds = value.split(':').run { get(0).toInt() to get(1).toInt() })
                    "--threshold" -> config.copy(threshold = value.toInt())
                    "--timeout" -> config.copy(timeout = value.toLong())
                    "--debug" -> config.copy(debug = value.toBoolean())
                    else -> config
                }
            }
        }
    }
}
