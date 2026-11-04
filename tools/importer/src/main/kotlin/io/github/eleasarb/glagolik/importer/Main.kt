package io.github.eleasarb.glagolik.importer

import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """
Usage: importer --input <verbs.csv> --overrides <overrides.csv> --ids <verb-ids.csv>
                --content-version <content-version.txt> --selection <selection.csv>
                --report <report.md> --db <verbs.db> [--core-size <n>] [--verify]
"""

fun main(args: Array<String>) {
    val options = parseArgs(args.toList())
    val paths = ImportPaths(
        input = File(options.require("--input")),
        overrides = File(options.require("--overrides")),
        ids = File(options.require("--ids")),
        contentVersion = File(options.require("--content-version")),
        selection = File(options.require("--selection")),
        report = File(options.require("--report")),
        database = File(options.require("--db")),
    )
    val coreSize = options.values["--core-size"]?.toInt() ?: DEFAULT_CORE_SIZE
    try {
        execute(paths, verify = options.verify, coreSize = coreSize)
        println(if (options.verify) "Verb data is up to date." else "Wrote ${paths.database}")
    } catch (e: ImportException) {
        System.err.println(e.message)
        exitProcess(1)
    }
}

private data class Options(val values: Map<String, String>, val verify: Boolean) {
    fun require(name: String): String = values[name] ?: usageError("missing $name")
}

private fun parseArgs(args: List<String>): Options {
    val values = mutableMapOf<String, String>()
    var verify = false
    var index = 0
    while (index < args.size) {
        val arg = args[index]
        if (arg == "--verify") {
            verify = true
            index++
        } else {
            values[arg] = args.getOrNull(index + 1) ?: usageError("missing value for $arg")
            index += 2
        }
    }
    return Options(values, verify)
}

private fun usageError(message: String): Nothing {
    System.err.println(message)
    System.err.println(USAGE.trimIndent())
    exitProcess(2)
}
