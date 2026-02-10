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

@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")

package net.ccbluex.liquidbounce.utils.render

import com.google.common.base.Suppliers
import com.google.common.util.concurrent.Runnables
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.util.Util
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

fun PoseStack.reset() {
    while (!isEmpty) popPose()
    setIdentity()
}

inline fun Tesselator.begin(pipeline: RenderPipeline): BufferBuilder =
    begin(pipeline.vertexFormatMode, pipeline.vertexFormat)

inline fun ByteBufferBuilder.begin(pipeline: RenderPipeline): BufferBuilder =
    BufferBuilder(this, pipeline.vertexFormatMode, pipeline.vertexFormat)

inline fun withOutputTextureOverride(
    color: GpuTextureView? = null,
    depth: GpuTextureView? = null,
    block: () -> Unit,
) {
    val oldColor = RenderSystem.outputColorTextureOverride
    val oldDepth = RenderSystem.outputDepthTextureOverride

    try {
        RenderSystem.outputColorTextureOverride = color
        RenderSystem.outputDepthTextureOverride = depth
        block()
    } finally {
        RenderSystem.outputColorTextureOverride = oldColor
        RenderSystem.outputDepthTextureOverride = oldDepth
    }
}

inline fun GpuTexture.clearColor(color: Int = 0) =
    gpuDevice.createCommandEncoder().clearColorTexture(this, color)

inline fun GpuTexture.clearDepth(depth: Double = 1.0) =
    gpuDevice.createCommandEncoder().clearDepthTexture(this, depth)

inline fun RenderTarget.clearColorAndDepth(color: Int = 0, depth: Double = 1.0) {
    val colorAttachment = colorTexture
    val depthAttachment = depthTexture.takeIf { useDepth }

    when {
        colorAttachment != null && depthAttachment != null ->
            gpuDevice.createCommandEncoder().clearColorAndDepthTextures(
                colorAttachment, color, depthAttachment, depth
            )
        colorAttachment != null -> colorAttachment.clearColor(color)
        depthAttachment != null -> depthAttachment.clearDepth(depth)
    }
}

inline fun GpuTexture.asView(baseMipLevel: Int = 0, mipLevels: Int = this.mipLevels): GpuTextureView =
    gpuDevice.createTextureView(this, baseMipLevel, mipLevels)

inline fun GpuBuffer.mapBuffer(read: Boolean, write: Boolean): GpuBuffer.MappedView =
    gpuDevice.createCommandEncoder().mapBuffer(this, read, write)

inline fun GpuBufferSlice.mapBuffer(read: Boolean, write: Boolean): GpuBuffer.MappedView =
    gpuDevice.createCommandEncoder().mapBuffer(this, read, write)

inline fun GpuBufferSlice.write(byteBuffer: ByteBuffer) =
    gpuDevice.createCommandEncoder().writeToBuffer(this, byteBuffer)

inline fun GpuBufferSlice.copyFrom(source: GpuBufferSlice) =
    gpuDevice.createCommandEncoder().copyToBuffer(this, source)

@Suppress("LongParameterList")
inline fun GpuTexture.write(
    source: NativeImage,
    mipLevel: Int = 0,
    depthOrLayer: Int = 0,
    destX: Int = 0,
    destY: Int = 0,
    width: Int = getWidth(mipLevel),
    height: Int = getWidth(mipLevel),
    sourceX: Int = 0,
    sourceY: Int = 0,
) = gpuDevice.createCommandEncoder().writeToTexture(
    this, source,
    mipLevel, depthOrLayer,
    destX, destY, width, height, sourceX, sourceY,
)

inline fun GpuTexture.copyTo(
    destination: GpuBuffer,
    offset: Long = 0L,
    mipLevel: Int = 0,
    x: Int = 0,
    y: Int = 0,
    width: Int = getWidth(0),
    height: Int = getHeight(0),
    callback: Runnable = Runnables.doNothing(),
) = gpuDevice.createCommandEncoder().copyTextureToBuffer(
    this, destination, offset, callback, mipLevel,
    x, y, width, height,
)

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
        nativeImage.writeToFile(file)
    }, Util.ioPool())

/**
 * @see net.minecraft.client.Screenshot.takeScreenshot
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
        width * height * pixelSize.toLong()
    )

    this.copyTo(gpuBuffer, mipLevel = mipLevel) {
        gpuBuffer.mapBuffer(read = true, write = false).use { mappedView ->
            val nativeImage = NativeImage(width, height, false)
            for (y in 0..<height) {
                for (x in 0..<width) {
                    val abgr = mappedView.data().getInt((x + y * width) * pixelSize)
                    nativeImage.setPixelABGR(x, height - y - 1, abgr)
                }
            }
            future.complete(nativeImage)
        }
        gpuBuffer.close()
    }

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
        width * height * pixelSize.toLong()
    )

    this.copyTo(gpuBuffer, mipLevel = mipLevel) {
        gpuBuffer.mapBuffer(read = true, write = false).use { mappedView ->
            val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0..<height) {
                for (x in 0..<width) {
                    val abgr = mappedView.data().getInt((x + y * width) * pixelSize)
                    bufferedImage.setRGB(x, height - y - 1, ARGB.fromABGR(abgr))
                }
            }
            future.complete(bufferedImage)
        }
        gpuBuffer.close()
    }

    return future
}

fun DynamicTexture.uploadRect(
    mipLevel: Int,
    x: Int, y: Int,
    width: Int, height: Int,
) = this.texture.write(
    source = this.pixels!!,
    mipLevel, depthOrLayer = 0,
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
        pixels,
        0,
        width
    )

    return bufferedImage
}

fun BufferedImage.toNativeImage(): NativeImage {
    val nativeImage = NativeImage(NativeImage.Format.RGBA, this.width, this.height, false)

    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            nativeImage.setPixel(x, y, this.getRGB(x, y))
        }
    }

    return nativeImage
}

fun NativeImage.registerTexture(identifier: Identifier) {
    mc.textureManager.register(identifier, asTexture(identifier::toString))
}

inline fun InputStream.toNativeImage(): NativeImage = NativeImage.read(this)

inline fun NativeImage.asTexture(
    name: String = "Texture NativeImage@${this.hashCode().toString(16)} (${this.width}x${this.height})",
) = DynamicTexture(Suppliers.ofInstance(name), this)

@JvmOverloads
fun NativeImage.asTexture(
    nameSupplier: Supplier<String> = Supplier {
        "Texture NativeImage@${this.hashCode().toString(16)} (${this.width}x${this.height})"
    },
) = DynamicTexture(nameSupplier, this)

val AbstractTexture.textureSetup: TextureSetup
    get() = TextureSetup.singleTexture(textureView, sampler)

inline fun GpuTextureView.asTextureSetup(sampler: GpuSampler): TextureSetup =
    TextureSetup.singleTexture(this, sampler)

inline fun ByteBuffer.toGpuBuffer(
    labelGetter: Supplier<String>? = null,
    usage: @GpuBuffer.Usage Int,
): GpuBuffer = gpuDevice.createBuffer(labelGetter, usage, this)

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
        std140Size(std140Size).toLong()
    )

inline fun ByteBuffer.writeStd140(action: Std140Builder.() -> Unit) {
    Std140Builder.intoBuffer(this).apply(action)
}

inline fun GpuBufferSlice.writeStd140(action: Std140Builder.() -> Unit): GpuBufferSlice =
    this.mapBuffer(read = false, write = true).use {
        it.data().writeStd140(action)

        this
    }

inline fun Std140Builder.putVec4(color: Color4b): Std140Builder =
    putVec4(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
