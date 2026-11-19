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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.buildText
import net.ccbluex.liquidbounce.utils.text.joinToText
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

private const val PROJECT_PACKAGE = "net.ccbluex.liquidbounce"

private val causeLabel = "Caused by: ".asPlainText(Style.EMPTY + ChatFormatting.RED + ChatFormatting.BOLD)
private val suppressedLabel = "Suppressed: ".asPlainText(Style.EMPTY + ChatFormatting.LIGHT_PURPLE + ChatFormatting.BOLD)

/**
 * Colors the stack trace the way an IDE does: the throwable red with its message orange, LiquidBounce's own frames blue
 * with their source location green, and everyone else's — the JDK, the game, libraries — gray.
 */
fun Throwable.stackTraceText(): Component =
    traceLines(indent = 0, label = null, seen = ReferenceOpenHashSet()).joinToText(CommonComponents.NEW_LINE)

private fun Throwable.traceLines(
    indent: Int,
    label: Component?,
    seen: ReferenceOpenHashSet<Throwable>,
): List<Component> {
    val lead = "  ".repeat(indent)

    // A throwable may enclose itself, which the JDK prints instead of the trace it would print again forever
    if (this in seen) {
        return [textOf(
            lead.asPlainText(ChatFormatting.DARK_GRAY),
            "[CIRCULAR REFERENCE: $this]".asPlainText(ChatFormatting.DARK_GRAY),
        )]
    }
    seen += this

    return buildList(stackTrace.size + 4) {
        this += buildText {
            this += lead.asPlainText(ChatFormatting.DARK_GRAY)
            this += label
            this += this@traceLines.javaClass.name.asPlainText(Style.EMPTY + ChatFormatting.RED + ChatFormatting.BOLD)
            this += message?.let { ": $it".asPlainText(ChatFormatting.GOLD) }
        }

        stackTrace.forEach { add(frameText("$lead  ", it)) }

        suppressed.forEach {
            addAll(it.traceLines(indent + 1, suppressedLabel, seen))
        }

        cause?.let {
            addAll(it.traceLines(indent, causeLabel, seen))
        }
    }
}

private fun frameText(lead: String, frame: StackTraceElement): Component {
    val own = frame.className.startsWith(PROJECT_PACKAGE)
    val classColor = if (own) ChatFormatting.BLUE else ChatFormatting.DARK_GRAY
    val methodColor = if (own) ChatFormatting.AQUA else ChatFormatting.DARK_GRAY

    return buildText {
        this += lead.asPlainText(ChatFormatting.DARK_GRAY)
        this += "at ".asPlainText(ChatFormatting.GRAY)
        frame.moduleName?.let { this += "$it/".asPlainText(ChatFormatting.DARK_GRAY) }
        this += frame.className.asPlainText(classColor)
        this += ".".asPlainText(ChatFormatting.DARK_GRAY)
        this += frame.methodName.asPlainText(methodColor)
        this += sourceText(frame)
    }
}

/**
 * The `(Foo.kt:12)` behind a frame, or `(Native Method)` and `(Unknown Source)` where it has no source.
 */
private fun sourceText(frame: StackTraceElement): Component {
    val fileName = frame.fileName

    return buildText {
        this += "(".asPlainText(ChatFormatting.DARK_GRAY)

        when {
            frame.isNativeMethod -> this += "Native Method".asPlainText(ChatFormatting.DARK_GRAY)
            fileName == null || frame.lineNumber < 0 ->
                this += "Unknown Source".asPlainText(ChatFormatting.DARK_GRAY)
            else -> {
                this += fileName.asPlainText(ChatFormatting.GREEN)
                this += ":".asPlainText(ChatFormatting.DARK_GRAY)
                this += frame.lineNumber.toString().asPlainText(Style.EMPTY + ChatFormatting.GREEN + ChatFormatting.BOLD)
            }
        }

        this += ")".asPlainText(ChatFormatting.DARK_GRAY)
    }
}
