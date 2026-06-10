/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CompareJsonKeysTask : DefaultTask() {

    /**
     * Baseline file
     */
    @get:InputFile
    abstract val baselineFile: RegularFileProperty

    /**
     * Files to check
     */
    @get:InputFiles
    abstract val files: ConfigurableFileCollection

    /**
     * Logger output limitation of missing keys
     */
    @get:Input
    abstract val consoleOutputCount: Property<Int>

    init {
        consoleOutputCount.convention(Int.MAX_VALUE)
    }

    @TaskAction
    fun run() {
        val baselineFile = baselineFile.orNull?.asFile

        if (baselineFile == null || !baselineFile.exists()) {
            throw GradleException("Baseline file $baselineFile not found")
        }

        @Suppress("UNCHECKED_CAST")
        fun File.readJsonObject() = inputStream().use(JsonSlurper()::parse) as Map<String, String>

        val baseline = baselineFile.readJsonObject()

        val outputCount = consoleOutputCount.get().coerceAtLeast(1)

        for (file in files.files) {
            if (file == baselineFile) {
                continue
            }

            val currentFile = file.readJsonObject()

            val missingKeys = baseline.keys - currentFile.keys

            if (missingKeys.isEmpty()) {
                logger.info("${file.name} is complete. No missing keys.")
            } else {
                val output = missingKeys.joinToString(
                    separator = ", ",
                    limit = outputCount,
                    truncated = "..."
                )
                logger.warn("${file.name} is missing the following keys (${missingKeys.size}): $output")
            }
        }
    }

}

/**
 * Converts a Fabric access widener (v1, official/Mojang names) into a NeoForge
 * access transformer (accesstransformer.cfg).
 *
 * The access widener remains the single source of truth; the NeoForge module
 * generates its access transformer from it at build time.
 *
 * Mapping:
 * - `accessible class a/b/C`        -> `public a.b.C`
 * - `accessible field C name desc`  -> `public a.b.C name`
 * - `accessible method C name desc` -> `public a.b.C name(desc)`
 * - `mutable field C name desc`     -> `public-f a.b.C name`
 *
 * `mutable` has no exact equivalent in access transformers, which always pair
 * definalization with an access level; `public-f` is a strict superset of the
 * Fabric behavior.
 */
abstract class ConvertAccessWidenerTask : DefaultTask() {

    @get:InputFile
    abstract val accessWidener: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun run() {
        val file = accessWidener.get().asFile
        val lines = file.readLines()

        val header = lines.firstOrNull()?.split(WHITESPACE).orEmpty()
        if (header.size < 3 || header[0] != "accessWidener" || header[1] != "v1" || header[2] != "official") {
            throw GradleException("Unsupported access widener header in $file: ${lines.firstOrNull()}")
        }

        // Entry (class + optional member) -> definalize flag, preserving first-seen order
        val entries = LinkedHashMap<String, Boolean>()

        for ((index, raw) in lines.withIndex().drop(1)) {
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) {
                continue
            }

            fun fail(reason: String): Nothing =
                throw GradleException("$file:${index + 1}: $reason: '$raw'")

            val parts = line.split(WHITESPACE)
            val access = parts[0]
            val kind = parts.getOrNull(1) ?: fail("missing member kind")

            val entry = when (kind) {
                "class" -> {
                    if (parts.size != 3) fail("malformed class entry")
                    parts[2].toBinaryName()
                }
                "field" -> {
                    if (parts.size != 5) fail("malformed field entry")
                    "${parts[2].toBinaryName()} ${parts[3]}"
                }
                "method" -> {
                    if (parts.size != 5) fail("malformed method entry")
                    "${parts[2].toBinaryName()} ${parts[3]}${parts[4]}"
                }
                else -> fail("unsupported member kind '$kind'")
            }

            when (access) {
                "accessible" -> entries.merge(entry, false, Boolean::or)
                "mutable" -> {
                    if (kind != "field") fail("'mutable' is only valid for fields")
                    entries.merge(entry, true, Boolean::or)
                }
                else -> fail("unsupported access type '$access' (only accessible/mutable are handled)")
            }
        }

        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(buildString {
            appendLine("# Generated from ${file.name} - do not edit, edit the access widener instead.")
            for ((entry, definalize) in entries) {
                appendLine(if (definalize) "public-f $entry" else "public $entry")
            }
        })
    }

    private fun String.toBinaryName() = replace('/', '.')

    private companion object {
        private val WHITESPACE = Regex("\\s+")
    }

}

/**
 * Verifies that the loader-agnostic sources (src/main) do not import loader-specific
 * packages. This keeps the Fabric/NeoForge source split honest without requiring a
 * NeoForge build for fast feedback.
 */
abstract class CheckLoaderPurityTask : DefaultTask() {

    @get:InputFiles
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val forbiddenPackages: ListProperty<String>

    @TaskAction
    fun run() {
        val forbidden = forbiddenPackages.get()
        val violations = mutableListOf<String>()

        for (file in sources.asFileTree) {
            if (file.extension != "kt" && file.extension != "java") {
                continue
            }

            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    val import = when {
                        trimmed.startsWith("import static ") -> trimmed.removePrefix("import static ")
                        trimmed.startsWith("import ") -> trimmed.removePrefix("import ")
                        else -> return@forEachIndexed
                    }

                    if (forbidden.any { import.startsWith(it) }) {
                        violations += "${file.path}:${index + 1}: $trimmed"
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Loader-specific imports found in loader-agnostic sources " +
                    "(move the code to a loader source set or behind the Platform interface):\n" +
                    violations.joinToString("\n")
            )
        }
    }

}
