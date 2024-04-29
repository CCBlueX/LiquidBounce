/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.client

import net.minecraft.nbt.NbtString
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.text.*
import net.minecraft.world.World
import java.util.*
import java.util.regex.Pattern

private val COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]")

fun String.stripMinecraftColorCodes(): String {
    return COLOR_PATTERN.matcher(this).replaceAll("")
}

fun text(): MutableText = Text.literal("")

fun String.asText(): MutableText = Text.literal(this)

fun Text.asNbt(world: World? = null): NbtString = NbtString.of(Text.Serialization.toJsonString(this, world?.registryManager ?: DynamicRegistryManager.EMPTY))

fun Text.convertToString(): String = "${string}${siblings.joinToString(separator = "") { it.convertToString() }}"

fun OrderedText.toText(): Text {
    val textSnippets = mutableListOf<Pair<String, Style>>()

    var currentStyle = Style.EMPTY
    val currentText = StringBuilder()

    this.accept { index, style, codePoint ->
        if (style != currentStyle) {
            if (currentText.isNotEmpty()) {
                textSnippets.add(currentText.toString() to currentStyle)
            }

            currentStyle = style

            currentText.clear()
        }

        currentText.append(codePoint.toChar())

        return@accept true
    }

    if (currentText.isNotEmpty()) {
        textSnippets.add(currentText.toString() to currentStyle)
    }

    if (textSnippets.isEmpty()) {
        return Text.empty()
    }

    val text = MutableText.of(PlainTextContent.of(textSnippets[0].first)).setStyle(textSnippets[0].second)

    for (i in 1 until textSnippets.size) {
        val (snippet, style) = textSnippets[i]

        text.append(MutableText.of(PlainTextContent.of(snippet)).setStyle(style))
    }

    return text
}

fun Text.processContent(): Text {
    val content = this.content

    if (content is TranslatableTextContent) {
        return MutableText.of(content.toPlainContent())
            .styled { style }
            .apply {
                for (child in siblings) {
                    append(child.processContent())
                }
            }
    }

    return this
}

fun TranslatableTextContent.toPlainContent(): TextContent {
    val stringBuilder = StringBuilder()

    visit {
        stringBuilder.append(it)

        Optional.empty<Any?>()
    }

    return PlainTextContent.of(stringBuilder.toString())
}

/**
 * Translate alt color codes to minecraft color codes
 */
fun String.translateColorCodes(): String {
    val charset = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr"

    val chars = toCharArray()
    for (i in 0 until chars.size - 1) {
        if (chars[i] == '&' && charset.contains(chars[i + 1], true)) {
            chars[i] = '§'
            chars[i + 1] = chars[i + 1].lowercaseChar()
        }
    }

    return String(chars)
}

fun String.toLowerCamelCase() = this.replaceFirst(this.toCharArray()[0], this.toCharArray()[0].lowercaseChar())

fun String.dropPort(): String {
    val parts = this.split(":")
    return parts[0]
}

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

    if (domain.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$"))) {
        // IP address
        return domain
    }

    // Check if domain ends with dot, if so, remove it
    if (domain.endsWith(".")) {
        domain = domain.dropLast(1)
    }

    val parts = domain.split(".")
    if (parts.size <= 2) {
        // Already a root domain
        return domain
    }

    return parts.takeLast(2).joinToString(".")
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
