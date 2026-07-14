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
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class RenderExtensionsTest {

    @Test
    fun testCopyIntArgbSubImageToNativeImage() {
        val parent = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        val expected = intArrayOf(0x10203040, 0x50607080, 0x90A0B0C0.toInt(), 0xD0E0F001.toInt())
        parent.setRGB(1, 1, 2, 2, expected, 0, 2)
        val source = parent.getSubimage(1, 1, 2, 2)

        NativeImage(2, 2, true).use { target ->
            val returnedScratch = source.copyToNativeImage(target, width = 2, height = 2)

            assertEquals(0, returnedScratch.size)
            assertArrayEquals(expected, target.pixels)
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
