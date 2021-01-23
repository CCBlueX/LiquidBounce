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

fun text() = LiteralText("")

fun String.asText() = LiteralText(this)

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
