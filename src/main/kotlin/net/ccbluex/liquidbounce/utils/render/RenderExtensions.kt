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

package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.ScreenshotRecorder
import org.apache.commons.lang3.mutable.MutableObject
import java.awt.image.BufferedImage

/**
 * @see ScreenshotRecorder.takeScreenshot
 */
fun Framebuffer.toNativeImage(): NativeImage {
    val ref = MutableObject<NativeImage>()
    // FIXME: check if alpha is cleared, this function disallow transparent pixel at 1.21.4
    ScreenshotRecorder.takeScreenshot(this, ref::setValue)
    return ref.value
}

fun NativeImage.toBufferedImage(): BufferedImage {
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    bufferedImage.setRGB(
        0,
        0,
        width,
        height,
        copyPixelsArgb(),
        0,
        width
    )

    return bufferedImage
}

fun BufferedImage.toNativeImage(): NativeImage {
    val nativeImage = NativeImage(NativeImage.Format.RGBA, this.width, this.height, false)

    // Fuck Minecraft native image
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            nativeImage.setColorArgb(x, y, this.getRGB(x, y))
        }
    }

    return nativeImage
}
