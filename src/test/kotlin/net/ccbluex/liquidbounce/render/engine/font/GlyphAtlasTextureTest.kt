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

package net.ccbluex.liquidbounce.render.engine.font

import com.mojang.blaze3d.platform.NativeImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class GlyphAtlasTextureTest {

    @Test
    fun testCopyIntArgbAlphaToLuminanceImage() {
        val parent = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        val source = parent.getSubimage(1, 1, 2, 2)
        source.setRGB(0, 0, 2, 2, ALPHA_PIXELS, 0, 2)

        NativeImage(NativeImage.Format.LUMINANCE, 2, 2, true).use { target ->
            val returnedScratch = source.copyCoverageToNativeImage(target, width = 2, height = 2)

            assertEquals(0, returnedScratch.size)
            assertLuminanceEquals(target, 0, 0, 0x10)
            assertLuminanceEquals(target, 1, 0, 0x50)
            assertLuminanceEquals(target, 0, 1, 0x90)
            assertLuminanceEquals(target, 1, 1, 0xD0)
        }
    }

    @Test
    fun testFallbackAlphaCopyReusesScratchBuffer() {
        val source = BufferedImage(2, 2, BufferedImage.TYPE_4BYTE_ABGR)
        source.setRGB(0, 0, 2, 2, ALPHA_PIXELS, 0, 2)
        val scratch = IntArray(4)

        NativeImage(NativeImage.Format.LUMINANCE, 3, 3, true).use { target ->
            val returnedScratch = source.copyCoverageToNativeImage(
                target,
                targetX = 1,
                targetY = 1,
                width = 2,
                height = 2,
                scratchBuffer = scratch,
            )

            assertSame(scratch, returnedScratch)
            assertLuminanceEquals(target, 1, 1, 0x10)
            assertLuminanceEquals(target, 2, 1, 0x50)
            assertLuminanceEquals(target, 1, 2, 0x90)
            assertLuminanceEquals(target, 2, 2, 0xD0)
        }
    }

    @Test
    fun testCopyByteGraySubImageToLuminanceImage() {
        val parent = BufferedImage(4, 4, BufferedImage.TYPE_BYTE_GRAY)
        val source = parent.getSubimage(1, 1, 2, 2)
        source.raster.setSample(0, 0, 0, 0x10)
        source.raster.setSample(1, 0, 0, 0x50)
        source.raster.setSample(0, 1, 0, 0x90)
        source.raster.setSample(1, 1, 0, 0xD0)

        NativeImage(NativeImage.Format.LUMINANCE, 2, 2, true).use { target ->
            val returnedScratch = source.copyCoverageToNativeImage(target, width = 2, height = 2)

            assertEquals(0, returnedScratch.size)
            assertLuminanceEquals(target, 0, 0, 0x10)
            assertLuminanceEquals(target, 1, 0, 0x50)
            assertLuminanceEquals(target, 0, 1, 0x90)
            assertLuminanceEquals(target, 1, 1, 0xD0)
        }
    }

    private fun assertLuminanceEquals(image: NativeImage, x: Int, y: Int, expected: Int) {
        assertEquals(expected, image.getLuminanceOrAlpha(x, y).toUByte().toInt())
    }

    companion object {
        private val ALPHA_PIXELS = intArrayOf(0x10203040, 0x50607080, 0x90A0B0C0.toInt(), 0xD0E0F001.toInt())
    }
}
