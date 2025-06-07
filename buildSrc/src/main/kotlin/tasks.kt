/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
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
