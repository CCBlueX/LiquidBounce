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
package net.ccbluex.liquidbounce.features.client.view

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * The JPEG frames of one client's view, and when someone last asked for one.
 */
internal class ClientViewFrames {

    @Volatile
    var latest: ByteArray? = null
        private set

    @Volatile
    var askedAt = System.nanoTime()

    fun encode(image: BufferedImage, done: () -> Unit) {
        encoder.execute {
            try {
                latest = jpeg(image)
            } finally {
                done()
            }
        }
    }

    companion object {

        private const val QUALITY = 0.75f

        private val encoder = Executors.newSingleThreadExecutor {
            Thread(it, "Client view encoder").apply { isDaemon = true }
        }

        // Only used on the encoder thread
        private val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        private val parameters = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = QUALITY
        }
        private val output = ByteArrayOutputStream()

        private fun jpeg(image: BufferedImage): ByteArray {
            output.reset()
            ImageIO.createImageOutputStream(output).use { stream ->
                writer.output = stream
                writer.write(null, IIOImage(image, null, null), parameters)
            }
            return output.toByteArray()
        }

    }

}
