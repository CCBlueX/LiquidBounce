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

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.ccbluex.liquidbounce.render.engine.type.Color4b
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
import net.minecraft.util.math.ColorHelper
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
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

inline fun GpuTexture.asView(): GpuTextureView =
    gpuDevice.createTextureView(this)

inline fun GpuBuffer.mapBuffer(read: Boolean, write: Boolean): GpuBuffer.MappedView =
    gpuDevice.createCommandEncoder().mapBuffer(this, read, write)

inline fun GpuBufferSlice.mapBuffer(read: Boolean, write: Boolean): GpuBuffer.MappedView =
    gpuDevice.createCommandEncoder().mapBuffer(this, read, write)

@JvmOverloads
fun GpuTexture.copyFully(
    labelGetter: Supplier<String>? = null,
    usage: Int = 0,
): GpuTexture {
    val dest = gpuDevice.createTexture(
        labelGetter,
        GpuTexture.USAGE_COPY_DST or usage,
        format,
        getWidth(0), getHeight(0),
        depthOrLayers, mipLevels,
    )

    for (mipLevel in 0 until mipLevels) {
        dest.copyFrom(this, mipLevel)
    }

    return dest
}

@Suppress("LongParameterList")
inline fun GpuTexture.copyFrom(
    source: GpuTexture,
    mipLevel: Int = 0,
    intoX: Int = 0,
    intoY: Int = 0,
    sourceX: Int = 0,
    sourceY: Int = 0,
    width: Int = source.getWidth(mipLevel),
    height: Int = source.getHeight(mipLevel),
) = gpuDevice.createCommandEncoder().copyTextureToTexture(
    source, this, mipLevel, intoX, intoY, sourceX, sourceY, width, height
)

fun GpuTexture.saveToFile(file: File): CompletableFuture<*> =
    this.toNativeImage().thenAcceptAsync({ nativeImage ->
        nativeImage.writeTo(file)
    }, Util.getIoWorkerExecutor())

/**
 * @see ScreenshotRecorder.takeScreenshot
 */
@JvmOverloads
fun GpuTexture.toNativeImage(mipLevel: Int = 0): CompletableFuture<NativeImage> {
    val future = CompletableFuture<NativeImage>()
    val width = this.getWidth(mipLevel)
    val height = this.getHeight(mipLevel)
    val pixelSize = this.format.pixelSize()
    val gpuBuffer = gpuDevice.createBuffer(
        { "PixelBuffer - " + (this.label ?: "Anonymous") },
        GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
        width * height * pixelSize
    )

    gpuDevice.createCommandEncoder().copyTextureToBuffer(this, gpuBuffer, 0, {
        gpuBuffer.mapBuffer(read = true, write = false).use { mappedView ->
            val nativeImage = NativeImage(width, height, false)
            for (y in 0..<height) {
                for (x in 0..<width) {
                    val abgr = mappedView.data().getInt((x + y * width) * pixelSize)
                    nativeImage.setColor(x, height - y - 1, abgr)
                }
            }
            future.complete(nativeImage)
        }
        gpuBuffer.close()
    }, mipLevel)

    return future
}

@JvmOverloads
fun GpuTexture.toBufferedImage(mipLevel: Int = 0): CompletableFuture<BufferedImage> {
    val future = CompletableFuture<BufferedImage>()
    val width = this.getWidth(mipLevel)
    val height = this.getHeight(mipLevel)
    val pixelSize = this.format.pixelSize()
    val gpuBuffer = gpuDevice.createBuffer(
        { "PixelBuffer - " + (this.label ?: "Anonymous") },
        GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
        width * height * pixelSize
    )

    gpuDevice.createCommandEncoder().copyTextureToBuffer(this, gpuBuffer, 0, {
        gpuBuffer.mapBuffer(read = true, write = false).use { mappedView ->
            val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0..<height) {
                for (x in 0..<width) {
                    val abgr = mappedView.data().getInt((x + y * width) * pixelSize)
                    bufferedImage.setRGB(x, height - y - 1, ColorHelper.fromAbgr(abgr))
                }
            }
            future.complete(bufferedImage)
        }
        gpuBuffer.close()
    }, mipLevel)

    return future
}

fun NativeImageBackedTexture.uploadRect(
    mipLevel: Int,
    x: Int, y: Int,
    width: Int, height: Int,
) = gpuDevice.createCommandEncoder().writeToTexture(
    this.glTexture, this.image!!,
    mipLevel, 0,
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
        GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST,
        it.buffer
    )
}

@JvmInline
value class KStd140SizeCalculator(val j: Std140SizeCalculator) {
    inline val float: Unit
        get() {
            j.putFloat()
        }
    inline val int: Unit
        get() {
            j.putInt()
        }
    inline val vec2: Unit
        get() {
            j.putVec2()
        }
    inline val ivec2: Unit
        get() {
            j.putIVec2()
        }
    inline val vec3: Unit
        get() {
            j.putVec3()
        }
    inline val ivec3: Unit
        get() {
            j.putIVec3()
        }
    inline val vec4: Unit
        get() {
            j.putIVec4()
        }
    inline val mat4f: Unit
        get() {
            j.putMat4f()
        }

    inline fun align(alignedSize: Int) {
        j.align(alignedSize)
    }

    inline operator fun Unit.plus(other: Unit) {
        // NOOP
    }

    inline fun get() = j.get()
}

inline fun std140Size(block: KStd140SizeCalculator.() -> Unit): Int =
    KStd140SizeCalculator(Std140SizeCalculator()).apply(block).get()

inline fun GpuDevice.createUbo(
    labelGetter: Supplier<String>? = null,
    std140Size: KStd140SizeCalculator.() -> Unit,
): GpuBuffer =
    createBuffer(
        labelGetter,
        GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_MAP_WRITE,
        std140Size(std140Size)
    )

inline fun ByteBuffer.writeStd140(): Std140Builder = Std140Builder.intoBuffer(this)

inline fun GpuBufferSlice.writeStd140(action: Std140Builder.() -> Unit): GpuBufferSlice =
    this.mapBuffer(read = false, write = true).use {
        it.data().writeStd140().apply(action)

        this
    }

inline fun Std140Builder.putVec4(color: Color4b): Std140Builder =
    putVec4(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
