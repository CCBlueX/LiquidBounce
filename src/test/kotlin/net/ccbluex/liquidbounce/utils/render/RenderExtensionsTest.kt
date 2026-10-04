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

package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.platform.NativeImage
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.Test
import java.awt.image.BufferedImage

class RenderExtensionsTest {

    private class TrackedInput(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closes = 0
            private set

        override fun close() {
            closes++
            super.close()
        }
    }

    private fun imageBytes(format: String): ByteArray {
        val image = BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB)
        return ByteArrayOutputStream().use { output ->
            check(ImageIO.write(image, format, output))
            output.toByteArray()
        }
    }

    @Test
    fun `JPEG decoding closes the original source after conversion`() {
        val input = TrackedInput(imageBytes("jpeg"))

        input.source().buffer().readNativeImage().use { image ->
            assertEquals(2, image.width)
            assertEquals(3, image.height)
        }

        assertEquals(1, input.closes)
    }

    @Test
    fun `PNG decoding closes the original source`() {
        val input = TrackedInput(imageBytes("png"))

        input.source().buffer().readNativeImage().use { image ->
            assertEquals(2, image.width)
            assertEquals(3, image.height)
        }

        assertEquals(1, input.closes)
    }

    @Test
    fun `unsupported image formats close the source`() {
        val input = TrackedInput("not an image".toByteArray())

        assertFailsWith<IllegalArgumentException> { input.source().buffer().readNativeImage() }

        assertEquals(1, input.closes)
    }

    @Test
    fun `short image inputs close the source`() {
        val input = TrackedInput(byteArrayOf(0x01, 0x02))

        assertFailsWith<IllegalArgumentException> { input.source().buffer().readNativeImage() }

        assertEquals(1, input.closes)
    }

    @Test
    fun `invalid JPEG data closes the source when conversion fails`() {
        val input = TrackedInput(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00))

        assertFailsWith<IOException> { input.source().buffer().readNativeImage() }

        assertEquals(1, input.closes)
    }

    @Test
    fun `invalid PNG data closes the source when native decoding fails`() {
        val input = TrackedInput(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        assertFailsWith<IOException> { input.source().buffer().readNativeImage() }

        assertEquals(1, input.closes)
    }

    @Test
    fun testCopyIntArgbSubImageToNativeImage() {
        val parent = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        val expected = intArrayOf(0x10203040, 0x50607080, 0x90A0B0C0.toInt(), 0xD0E0F001.toInt())
        parent.setRGB(1, 1, 2, 2, expected, 0, 2)
        val source = parent.getSubimage(1, 1, 2, 2)

        NativeImage(2, 2, true).use { target ->
            val returnedScratch = source.copyToNativeImage(target, width = 2, height = 2)

            assertEquals(0, returnedScratch.size)
            assertContentEquals(expected, target.pixels)
        }
    }

    @Test
    fun testFallbackCopyReusesScratchBuffer() {
        val source = BufferedImage(2, 2, BufferedImage.TYPE_4BYTE_ABGR)
        val expected = intArrayOf(0xFF123456.toInt(), 0x80123456.toInt(), 0x40112233, 0x00112233)
        source.setRGB(0, 0, 2, 2, expected, 0, 2)
        val scratch = IntArray(4)

        NativeImage(3, 3, true).use { target ->
            val returnedScratch = source.copyToNativeImage(
                target,
                targetX = 1,
                targetY = 1,
                width = 2,
                height = 2,
                scratchBuffer = scratch,
            )

            assertSame(scratch, returnedScratch)
            assertEquals(expected[0], target.getPixel(1, 1))
            assertEquals(expected[1], target.getPixel(2, 1))
            assertEquals(expected[2], target.getPixel(1, 2))
            assertEquals(expected[3], target.getPixel(2, 2))
        }
    }
}
