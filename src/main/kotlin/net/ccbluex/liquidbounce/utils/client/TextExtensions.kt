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
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import java.util.regex.Pattern

private val COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]")

fun String.stripMinecraftColorCodes(): String {
    return COLOR_PATTERN.matcher(this).replaceAll("")
}

fun text(): MutableText = Text.literal("")

fun String.asText(): MutableText = Text.literal(this)

fun Text.asNbt(): NbtString = NbtString.of(Text.Serialization.toJsonString(this))

fun Text.convertToString(): String = "${string}${siblings.joinToString(separator = "") { it.convertToString() }}"

/**
 * Translate alt color codes to minecraft color codes
 */
fun String.translateColorCodes(): String {
    val charset = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr"

    val chars = toCharArray()
    for (i in 0 until chars.size - 1) {
        if (chars[i] == '&' && charset.contains(chars[i + 1], true)) {
            chars[i] = '§'
            chars[i + 1] = chars[i + 1].toLowerCase()
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
    val domain = this.trim().lowercase()

    if (domain.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$"))) {
        // IP address
        return domain
    }

    val parts = domain.split(".")
    if (parts.size <= 2) {
        // Already a root domain
        return domain
    }

    return parts.takeLast(2).joinToString(".")
}
