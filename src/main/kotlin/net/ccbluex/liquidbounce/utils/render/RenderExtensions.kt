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

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.ScreenshotRecorder
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * @see ScreenshotRecorder.takeScreenshot
 */
fun Framebuffer.toNativeImage(): NativeImage {
    val future = CompletableFuture<NativeImage>()
    val i: Int = textureWidth
    val j: Int = textureHeight
    val gpuTexture: GpuTexture? = getColorAttachment()
    checkNotNull(gpuTexture != null) { "Tried to capture screenshot of an incomplete framebuffer" }
    val gpuBuffer = RenderSystem.getDevice()
        .createBuffer(
            Supplier { "Screenshot buffer" },
            BufferType.PIXEL_PACK,
            BufferUsage.STATIC_READ,
            i * j * gpuTexture!!.format.pixelSize()
        )
    val commandEncoder = RenderSystem.getDevice().createCommandEncoder()
    RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0, Runnable {
        commandEncoder.readBuffer(gpuBuffer).use { readView ->
            val nativeImage = NativeImage(i, j, false)
            for (k in 0..<j) {
                for (l in 0..<i) {
                    val m = readView.data().getInt((l + k * i) * gpuTexture.format.pixelSize())
                    nativeImage.setColor(l, j - k - 1, m)
                }
            }
            future.complete(nativeImage)
        }
        gpuBuffer.close()
    }, 0)

    return future.get()
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
