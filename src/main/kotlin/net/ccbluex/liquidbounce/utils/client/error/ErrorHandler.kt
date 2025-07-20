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
@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.client.error

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.error.errors.ClientError
import net.ccbluex.liquidbounce.utils.client.mc
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.math.min
import kotlin.system.exitProcess

private typealias CurrentStringBuilder = StringBuilder

private const val MAX_STACKTRACE_LINES = 3

/**
 * The ErrorHandler class is responsible for handling and reporting errors encountered by the application.
 */
class ErrorHandler private constructor(
    private val error: Throwable,
    private val quickFix: QuickFix? = null,
    private val additionalMessage: String? = null,
    private val needToReport: Boolean = true
) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun fatal(
            error: Throwable,
            quickFix: QuickFix? = null,
            needToReport: Boolean = true,
            additionalMessage: String? = null,
        ): Nothing {
            val finalQuickFix = quickFix ?: QuickFix.entries.firstOrNull { it.testError(error) }

            val finalNeedToReport = if (error is ClientError) {
                error.needToReport
            } else {
                needToReport
            }

            ErrorHandler(error, finalQuickFix, additionalMessage, finalNeedToReport).apply {
                if (buildAndShowMessage()) {
                    browseUrl("https://github.com/CCBlueX/LiquidBounce/issues/new?template=bug_report.yml")
                }

                exitProcess(1)
            }
        }
    }

    private inline val title get() = "${LiquidBounce.CLIENT_NAME} Nextgen"

    private val builder = CurrentStringBuilder()

    private inline fun header(): CurrentStringBuilder = builder.append(
        "$title has encountered an error!"
    )

    private inline fun quickFix(): CurrentStringBuilder = builder.apply {
        requireNotNull(quickFix)

        append(quickFix.description)
        appendLine(2)

        val messages = quickFix.messages
            .map {
                it.key to (it.value!!.showStepIndex to it.value!!.steps(error))
            }.filter {
                it.second.second?.isEmpty() == false
            }

        for ((index, instructions) in messages.withIndex()) {
            val (title, instruction) = instructions
            val (showStepIndex, step) = instruction

            requireNotNull(step)

            append("${title}:")
            appendLine()
            appendQuickFixInstructionStep(showStepIndex, step)

            if (index < quickFix.messages.size - 1) {
                appendLine(2)
            }
        }
    }

    private inline fun reportMessage(): CurrentStringBuilder = builder.apply {
        append(
            """
                Try restarting the client.
                Please report this issue to the developers on GitHub if the error keeps occurring.

                Include the following information:
            """.trimIndent())
        appendLine(2)

        systemSpecs()
        appendLine()

        error()
        appendLine(2)

        append("Also include you game log, which can be found at:")
        appendLine()
        append((mc.runDirectory.toPath() / "logs" / "latest.log").absolutePathString())

        appendLine(2)
        append("Open new GitHub issue?")
    }

    private inline fun systemSpecs(): CurrentStringBuilder = builder.append(
        """
            OS: ${System.getProperty("os.name")} (${System.getProperty("os.arch")})
            Java: ${System.getProperty("java.version")}
            Client version: ${LiquidBounce.clientVersion} (${LiquidBounce.clientCommit})
        """.trimIndent()
    )

    private inline fun error(): CurrentStringBuilder = builder.apply {
        append("Error: ${error.message} (${error.javaClass.name})")
        appendLine()
        stacktrace()

        if (additionalMessage != null) {
            appendLine()
            append("Additional message: $additionalMessage")
        }
    }

    @Suppress("UnusedPrivateProperty")
    private inline fun stacktrace(): CurrentStringBuilder = builder.apply {
        val elements = error.stackTrace.toList()

        val displayed = min(elements.size, MAX_STACKTRACE_LINES)
        val displayedItems = elements.take(displayed)

        displayedItems.withIndex().forEach { (idx, item) ->
            append("  at $item")

            if (idx < displayedItems.size-1) {
                appendLine()
            }
        }

        (elements.size - displayedItems.size)
            .takeIf { it > 0 }
            ?.let {
                appendLine()
                append("  ... and $it more")
            }
    }

    @Suppress("MagicNumber")
    fun buildAndShowMessage(): Boolean {
        builder.apply {
            header()
            appendLine(3)

            if (quickFix != null) {
                quickFix()
                appendLine(3)
            }

            if (needToReport) {
                reportMessage()
            } else {
                systemSpecs()
                appendLine()
                error()
            }
        }

        val message = builder.toString().replace("\"", "").replace("'", "")

        return if (needToReport) {
            TinyFileDialogs.tinyfd_messageBox(
                title,
                message,
                "yesno",
                "error",
                true
            )
        } else {
            TinyFileDialogs.tinyfd_messageBox(
                title,
                message,
                "ok",
                "error",
                true
            )

            false
        }
    }
}

private inline fun Appendable.appendQuickFixInstructionStep(
    showStepIndex: Boolean,
    steps: Array<String>
): Appendable = apply {
    steps
        .map {
            if (!it.endsWith(".")) {
                "$it."
            } else {
                it
            }
        }
        .withIndex()
        .joinToString("\n") { (index, line) ->
            val stepIndex = if (showStepIndex) {
                "${index + 1}."
            } else {
                "-"
            }

            "$stepIndex $line"
        }
        .let {
            if (it.isNotEmpty()) {
                append(it)
            }
        }
}

private inline fun Appendable.appendLine(times: Int = 1): Appendable = apply {
    repeat(times) {
        append('\n')
    }
}
