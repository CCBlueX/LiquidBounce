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

package net.ccbluex.liquidbounce.utils.io

import java.io.IOException
import java.io.RandomAccessFile
import java.util.BitSet
import javax.imageio.ImageIO
import okio.Buffer
import okio.BufferedSource

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

@Suppress("ThrowsCount")
@Throws(IOException::class, IllegalArgumentException::class)
fun BufferedSource.ensurePngOrConvertJpeg(): BufferedSource {
    if (!request(3)) {
        throw IllegalArgumentException("Unsupported image format: file is too short")
    }

    val sourceBuffer = buffer
    val isPng = request(8) &&
        sourceBuffer[0] == 0x89.toByte() &&
        sourceBuffer[1] == 0x50.toByte() &&
        sourceBuffer[2] == 0x4E.toByte() &&
        sourceBuffer[3] == 0x47.toByte() &&
        sourceBuffer[4] == 0x0D.toByte() &&
        sourceBuffer[5] == 0x0A.toByte() &&
        sourceBuffer[6] == 0x1A.toByte() &&
        sourceBuffer[7] == 0x0A.toByte()

    if (isPng) {
        return this
    }

    val isJpeg = sourceBuffer[0] == 0xFF.toByte() &&
        sourceBuffer[1] == 0xD8.toByte() &&
        sourceBuffer[2] == 0xFF.toByte()

    if (isJpeg) {
        val image = ImageIO.read(inputStream())
            ?: throw IllegalArgumentException("Failed to decode JPEG image")
        val output = Buffer()
        if (!ImageIO.write(image, "png", output.outputStream())) {
            throw IllegalArgumentException("Failed to encode PNG image")
        }
        return output
    }

    throw IllegalArgumentException("Unsupported image format: only PNG and JPEG are allowed")
}
