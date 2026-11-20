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
package net.ccbluex.liquidbounce.utils.client.error

import kotlinx.coroutines.future.future
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.thirdparty.LogsApi
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException
import net.minecraft.SharedConstants
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture

/**
 * What a bug report about a fatal error needs: the error, the environment and the uploaded log.
 */
internal class ErrorReport(error: Throwable, private val additionalMessage: String?) {

    // Minecraft reports what a screen or a tick throws wrapped in a crash report
    private val cause = (error as? ReportedException)?.cause ?: error

    val summary = cause.javaClass.simpleName.let { name -> cause.message?.let { "$it ($name)" } ?: name }

    val environment = listOf(
        "${LiquidBounce.CLIENT_NAME} ${LiquidBounce.clientVersion} (${LiquidBounce.clientCommit})",
        "Minecraft ${SharedConstants.getCurrentVersion().name()}",
        "Java ${System.getProperty("java.version")}",
        "${System.getProperty("os.name")} ${System.getProperty("os.arch")}",
    ).joinToString(" · ")

    /** The upload of `latest.log`, once started. */
    var log: CompletableFuture<String>? = null
        private set

    /**
     * Uploads the log unless it is uploaded or on the way.
     */
    fun uploadLog(): CompletableFuture<String> {
        log?.takeUnless { it.isCompletedExceptionally }?.let { return it }

        return ioScope.future {
            LogsApi.upload(checkNotNull(LogsApi.readLatestLog()) { "There is no log" })
        }.whenComplete { _, error ->
            error?.let { logger.error("Failed to upload the log", it) }
        }.also { log = it }
    }

    /**
     * A new issue from the bug report template, filled in with the error, the environment and [logUrl]. Holds as
     * much of the stack trace as fits into a link that every system still opens; the log has all of it.
     */
    fun issueUrl(logUrl: String?): String {
        val trace = cause.stackTraceToString().lines()
        var traceLines = TRACE_LINES
        while (true) {
            val url = issueUrl(logUrl, trace.take(traceLines).joinToString("\n"))
            if (url.length <= MAX_URL_LENGTH || traceLines == 0) {
                return url
            }
            traceLines--
        }
    }

    private fun issueUrl(logUrl: String?, trace: String): String {
        val description = buildString {
            append("The game stopped with an error: ${summary.take(SUMMARY_LENGTH)}")
            additionalMessage?.let { append("\n\nWhile: $it") }
        }
        val logField = buildString {
            appendLine(logUrl ?: "The log upload failed, logs/latest.log is attached.")
            appendLine()
            append(trace)
        }

        val fields = mapOf(
            "title" to "[BUG] ${summary.take(TITLE_LENGTH)}",
            "branch" to "Nextgen",
            "version" to "${LiquidBounce.clientVersion} (${LiquidBounce.clientCommit})",
            "os" to operatingSystem(),
            "minecraft" to SharedConstants.getCurrentVersion().name(),
            "description" to description,
            "log" to logField,
        )
        return ISSUE_URL + fields.entries.joinToString("") { (key, value) ->
            "&$key=${URLEncoder.encode(value, Charsets.UTF_8)}"
        }
    }

    /** One of the template's options, which match `os.name` except for macOS. */
    private fun operatingSystem(): String {
        val name = System.getProperty("os.name")
        return if (name.startsWith("Mac", ignoreCase = true)) "macOS" else name
    }

    private companion object {
        const val TRACE_LINES = 25
        const val TITLE_LENGTH = 80
        const val SUMMARY_LENGTH = 300

        /** Windows hands longer links to the browser cut off or not at all. */
        const val MAX_URL_LENGTH = 2000
    }

}
