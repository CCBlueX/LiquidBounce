/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import java.util.regex.Pattern

private val COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]")

fun String.stripMinecraftColorCodes(): String {
    return COLOR_PATTERN.matcher(this).replaceAll("")
}

fun text() = LiteralText("")

fun String.asText() = LiteralText(this)

fun Text.outputString(): String = "${asString()}${siblings.joinToString(separator = "") { it.outputString() }}"

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

// https://stackoverflow.com/questions/44315977/ranges-in-kotlin-using-data-type-double
infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}
