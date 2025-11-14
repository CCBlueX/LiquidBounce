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
@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")

package net.ccbluex.liquidbounce.utils.client

import com.google.common.base.CaseFormat
import it.unimi.dsi.fastutil.chars.CharOpenHashSet
import net.ccbluex.fastutil.unmodifiable
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import net.minecraft.text.*
import net.minecraft.util.Formatting
import java.util.*
import java.util.regex.Pattern

private val COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]")

fun String.stripMinecraftColorCodes(): String {
    return COLOR_PATTERN.matcher(this).replaceAll("")
}

inline fun String.asTextContent(): TextContent = PlainTextContent.of(this)

/**
 * Returns a [MutableText] from the receiver.
 * If you just need a [Text], use [asPlainText] instead.
 */
inline fun String.asText(): MutableText = Text.literal(this)

/**
 * Returns an immutable [Text] from the receiver.
 */
inline fun String.asPlainText(): Text = PlainText.of(this, Style.EMPTY)

/**
 * Returns an immutable [Text] from the receiver with [style].
 */
inline fun String.asPlainText(style: Style): Text = PlainText.of(this, style)

/**
 * Returns an immutable [Text] from the receiver with [formatting].
 */
inline fun String.asPlainText(formatting: Formatting): Text = PlainText.of(this, formatting)

inline fun List<Text>.asText(): Text = TextList.of(this)

inline fun Array<out Text>.asText(): Text = TextList.of(this.unmodifiable())

inline fun textOf(vararg parts: Text): Text = parts.asText()

fun OrderedText.toText(): Text {
    if (this is Text) return this

    val parts = mutableListOf<Text>()

    var currentStyle = Style.EMPTY
    val currentText = Pools.StringBuilder.borrow()

    this.accept { _, style, codePoint ->
        if (style != currentStyle) {
            if (currentText.isNotEmpty()) {
                parts += currentText.toString().asPlainText(currentStyle)
            }

            currentStyle = style

            currentText.setLength(0)
        }

        currentText.appendCodePoint(codePoint)

        return@accept true
    }

    if (currentText.isNotEmpty()) {
        parts += currentText.toString().asPlainText(currentStyle)
    }

    Pools.StringBuilder.recycle(currentText)

    return parts.asText()
}

fun Text.processContent(): Text {
    fun translateOrSelf(content: TextContent): TextContent =
        (content as? TranslatableTextContent)?.toTranslatedString()?.asTextContent() ?: content

    val content = this.content
    val processedContent = translateOrSelf(content)

    val processedSiblings = siblings.map(Text::processContent)

    return if (processedContent === content && processedSiblings == siblings) {
        this
    } else {
        MutableText.of(processedContent).setStyle(style).apply {
            siblings.addAll(processedSiblings)
        }
    }
}

fun TranslatableTextContent.toTranslatedString(): String = buildString {
    visit {
        append(it)

        Optional.empty<Nothing>()
    }
}

private val COLOR_CODE_CHARS = CharOpenHashSet("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".toCharArray()).unmodifiable()

/**
 * Translate alt color codes to minecraft color codes
 */
fun String.translateColorCodes(): String {
    val chars = toCharArray()
    for (i in 0 until chars.lastIndex) {
        if (chars[i] == '&' && COLOR_CODE_CHARS.contains(chars[i + 1])) {
            chars[i] = '§'
            chars[i + 1] = chars[i + 1].lowercaseChar()
        }
    }

    return String(chars)
}

fun String.capitalize(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

fun String.toLowerCamelCase(): String = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, this)

fun String.dropPort(): String {
    return this.substringBefore(':')
}

private val IP_REGEX = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")

/**
 * Returns the root domain of the domain.
 *
 * This means it removes the subdomain from the domain.
 * If the domain is already a root domain or an IP address, do nothing.
 *
 * e.g.
 *   "sub.example.com" -> "example.com"
 *   "example.com." -> "example.com"
 *   "127.0.0.1" -> "127.0.0.1"
 */
fun String.rootDomain(): String {
    var domain = this.trim().lowercase()

    if (domain.matches(IP_REGEX)) {
        // IP address
        return domain
    }

    // Check if domain ends with dot, if so, remove it
    if (domain.endsWith('.')) {
        domain = domain.dropLast(1)
    }

    val parts = domain.split('.')
    if (parts.size <= 2) {
        // Already a root domain
        return domain
    }

    return "${parts[parts.lastIndex - 1]}.${parts.last()}"
}

/**
 * Converts milliseconds to seconds, minutes, hours and days when present.
 */
fun Int.formatAsTime(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m ${seconds % 60}s"
        hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

fun Long.formatAsCapacity(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "$this ${units[unitIndex]}"
    } else {
        "%.2f ${units[unitIndex]}".format(size)
    }
}

fun String.hideSensitiveAddress(): String {
    // Hide possibly sensitive information from LiquidProxy
    return when {
        this.endsWith(".liquidbounce.net") -> "<redacted>.liquidbounce.net"
        this.endsWith(".liquidproxy.net") -> "<redacted>.liquidproxy.net"
        else -> this
    }
}

@JvmRecord
data class ColoredChar(val char: Char, val color: Formatting) {
    init {
        requireNotNull(color.colorValue) { "The formatting must be a color formatting!" }
    }
}

inline fun Char.colored(color: Formatting) = ColoredChar(this, color)

fun Char.repeat(n: Int): String = CharArray(n) { this }.concatToString()

/**
 * Generates a progress bar based on the [percent]age (range 0 to 100).
 */
fun textLoadingBar(
    percent: Int,
    progress: ColoredChar = '█'.colored(Formatting.WHITE),
    remaining: ColoredChar = '░'.colored(Formatting.DARK_GRAY),
    length: Int = 10
): Text {
    val clampedPercent = percent.coerceIn(0, 100)
    val filledBars = clampedPercent * length / 100

    val progressPart = progress.char.repeat(filledBars)
    val remainingPart = remaining.char.repeat(length - filledBars)

    return textOf(
        progressPart.asPlainText(progress.color),
        remainingPart.asPlainText(remaining.color),
    )
}
