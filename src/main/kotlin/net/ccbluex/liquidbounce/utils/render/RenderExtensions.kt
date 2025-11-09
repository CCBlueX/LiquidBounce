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

@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.textures.GpuTexture
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.ScreenshotRecorder
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * Avoiding String contract
 */
@JvmField
val SAMPLER_NAMES = Array(12) { "Sampler$it" }

fun MatrixStack.reset() {
    while (!isEmpty) pop()
    loadIdentity()
}

inline fun GpuTexture.clearColor(color: Int) =
    gpuDevice.createCommandEncoder().clearColorTexture(this, color)

inline fun GpuTexture.clearDepth(depth: Double) =
    gpuDevice.createCommandEncoder().clearDepthTexture(this, depth)

inline fun Framebuffer.clearColorAndDepth(color: Int, depth: Double) =
    gpuDevice.createCommandEncoder().clearColorAndDepthTextures(colorAttachment, color, depthAttachment, depth)

@Suppress("LongParameterList")
inline fun GpuTexture.copyFrom(
    source: GpuTexture,
    mipLevel: Int = 0,
    intoX: Int = 0,
    intoY: Int = 0,
    sourceX: Int = 0,
    sourceY: Int = 0,
    width: Int = source.getWidth(0),
    height: Int = source.getHeight(0),
) = gpuDevice.createCommandEncoder().copyTextureToTexture(
    this, source, mipLevel, intoX, intoY, sourceX, sourceY, width, height
)

fun GpuTexture.saveToFile(file: File): CompletableFuture<*> =
    this.toNativeImage().thenAcceptAsync({ nativeImage ->
        nativeImage.writeTo(file)
    }, Util.getIoWorkerExecutor())

/**
 * @see ScreenshotRecorder.takeScreenshot
 */
fun GpuTexture.toNativeImage(): CompletableFuture<NativeImage> {
    val future = CompletableFuture<NativeImage>()
    val i = this.getWidth(0)
    val j = this.getHeight(0)
    val pixelSize = this.format.pixelSize()
    val gpuBuffer = gpuDevice
        .createBuffer(
            { "Screenshot buffer" },
            BufferType.PIXEL_PACK,
            BufferUsage.STATIC_READ,
            i * j * pixelSize
        )
    gpuDevice.createCommandEncoder().copyTextureToBuffer(this, gpuBuffer, 0, {
        gpuDevice.createCommandEncoder().readBuffer(gpuBuffer).use { readView ->
            val nativeImage = NativeImage(i, j, false)
            for (k in 0..<j) {
                for (l in 0..<i) {
                    val m = readView.data().getInt((l + k * i) * pixelSize)
                    nativeImage.setColor(l, j - k - 1, m)
                }
            }
            future.complete(nativeImage)
        }
        gpuBuffer.close()
    }, 0)

    return future
}

fun NativeImageBackedTexture.uploadRect(
    mipLevel: Int,
    x: Int, y: Int,
    width: Int, height: Int,
) = gpuDevice.createCommandEncoder().writeToTexture(
    this.glTexture, this.image!!,
    mipLevel,
    x, y,
    width, height,
    x, y,
)

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

fun NativeImage.registerTexture(identifier: Identifier) {
    mc.textureManager.registerTexture(identifier, asTexture(identifier::toString))
}

inline fun InputStream.toNativeImage(): NativeImage = NativeImage.read(this)

@JvmOverloads
inline fun NativeImage.asTexture(nameSupplier: Supplier<String>? = null) =
    NativeImageBackedTexture(nameSupplier, this)

@JvmOverloads
fun BuiltBuffer.createGpuBuffer(labelGetter: Supplier<String>? = null): GpuBuffer = use {
    gpuDevice.createBuffer(
        labelGetter,
        BufferType.VERTICES,
        BufferUsage.STATIC_WRITE,
        it.buffer
    )
}
