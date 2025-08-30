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

package net.ccbluex.liquidbounce.utils.io

import java.io.IOException
import java.io.RandomAccessFile

/**
 * Skips the current line in the file.
 *
 * @return The number of bytes skipped.
 */
@Throws(IOException::class)
fun RandomAccessFile.skipLine(): Long {
    var read = 0L
    var eol = false

    while (!eol) {
        when (read()) {
            -1, '\n'.code -> eol = true
            '\r'.code -> {
                eol = true
                val cur = filePointer
                if ((read()) != '\n'.code) {
                    seek(cur)
                }
            }

            else -> read++
        }
    }

    return read
}
