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

import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.render.write
import net.minecraft.client.renderer.texture.AbstractTexture
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.awt.image.ComponentSampleModel
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.awt.image.SinglePixelPackedSampleModel
import java.util.function.Supplier

class GlyphAtlasTexture(
    label: Supplier<String>,
    pixels: NativeImage,
    retainPixels: Boolean,
) : AbstractTexture() {

    var pixels: NativeImage? = pixels
        private set

    init {
        require(pixels.format() == NativeImage.Format.LUMINANCE) {
            "Glyph atlas pixels must use LUMINANCE format"
        }

        val device = gpuDevice
        val gpuTexture = device.createTexture(
            label,
            GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
            GpuFormat.R8_UNORM,
            pixels.width,
            pixels.height,
            1,
            1,
        )
        texture = gpuTexture
        sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)
        textureView = device.createTextureView(gpuTexture)

        try {
            upload(pixels)
        } catch (throwable: Throwable) {
            close()
            throw throwable
        }

        if (!retainPixels) {
            pixels.close()
            this.pixels = null
        }
    }

    fun upload() = upload(requireNotNull(pixels) { "Glyph atlas has no retained pixel data" })

    @Suppress("LongParameterList")
    fun uploadRect(
        mipLevel: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val pixels = requireNotNull(pixels) { "Glyph atlas has no retained pixel data" }
        getTexture().write(
            source = pixels,
            mipLevel = mipLevel,
            depthOrLayer = 0,
            destX = x,
            destY = y,
            width = width,
            height = height,
            sourceX = x,
            sourceY = y,
        )
    }

    private fun upload(source: NativeImage) {
        getTexture().write(source, width = source.width, height = source.height)
    }

    override fun close() {
        pixels?.close()
        pixels = null
        super.close()
    }
}

internal fun BufferedImage.toLuminanceNativeImage(): NativeImage {
    val nativeImage = NativeImage(NativeImage.Format.LUMINANCE, width, height, false)

    try {
        copyCoverageToNativeImage(nativeImage, width = width, height = height)
    } catch (throwable: Throwable) {
        nativeImage.close()
        throw throwable
    }

    return nativeImage
}

@Suppress("LongParameterList")
internal fun BufferedImage.copyCoverageToNativeImage(
    target: NativeImage,
    sourceX: Int = 0,
    sourceY: Int = 0,
    targetX: Int = 0,
    targetY: Int = 0,
    width: Int = this.width,
    height: Int = this.height,
    scratchBuffer: IntArray = IntArray(0),
): IntArray {
    validateCoverageCopy(target, sourceX, sourceY, targetX, targetY, width, height)

    val targetPixels = MemoryUtil.memByteBuffer(target.pointer, target.width * target.height)
    val dataBuffer = raster.dataBuffer
    val sampleModel = raster.sampleModel

    if (type == BufferedImage.TYPE_BYTE_GRAY &&
        dataBuffer is DataBufferByte && sampleModel is ComponentSampleModel
    ) {
        val sourcePixels = dataBuffer.data
        val sourceOffset = dataBuffer.offset +
            (sourceY - raster.sampleModelTranslateY) * sampleModel.scanlineStride +
            (sourceX - raster.sampleModelTranslateX) * sampleModel.pixelStride +
            sampleModel.bandOffsets[0]

        copyCoverageRows(
            sourcePixels,
            sourceOffset,
            sampleModel.scanlineStride,
            targetPixels,
            targetX + targetY * target.width,
            target.width,
            width,
            height,
        )
        return scratchBuffer
    }

    if (type == BufferedImage.TYPE_INT_ARGB &&
        dataBuffer is DataBufferInt && sampleModel is SinglePixelPackedSampleModel
    ) {
        val sourcePixels = dataBuffer.data
        val sourceOffset = dataBuffer.offset +
            (sourceY - raster.sampleModelTranslateY) * sampleModel.scanlineStride +
            sourceX - raster.sampleModelTranslateX

        copyAlphaRows(
            sourcePixels,
            sourceOffset,
            sampleModel.scanlineStride,
            targetPixels,
            targetX + targetY * target.width,
            target.width,
            width,
            height,
        )
        return scratchBuffer
    }

    val requiredSize = width * height
    val argbPixels = scratchBuffer.takeIf { it.size >= requiredSize } ?: IntArray(requiredSize)
    getRGB(sourceX, sourceY, width, height, argbPixels, 0, width)
    copyAlphaRows(
        argbPixels,
        0,
        width,
        targetPixels,
        targetX + targetY * target.width,
        target.width,
        width,
        height,
    )
    return argbPixels
}

@Suppress("LongParameterList")
private fun BufferedImage.validateCoverageCopy(
    target: NativeImage,
    sourceX: Int,
    sourceY: Int,
    targetX: Int,
    targetY: Int,
    width: Int,
    height: Int,
) {
    require(!target.isClosed) { "Target image is closed" }
    require(target.format() == NativeImage.Format.LUMINANCE) { "Target image must use LUMINANCE format" }
    require(width >= 0 && height >= 0) { "Copy dimensions must not be negative" }
    require(sourceX >= 0 && sourceY >= 0 && width <= this.width - sourceX && height <= this.height - sourceY) {
        "Source rectangle is outside the BufferedImage"
    }
    require(targetX >= 0 && targetY >= 0 && width <= target.width - targetX && height <= target.height - targetY) {
        "Target rectangle is outside the NativeImage"
    }
}

private fun copyCoverageRows(
    source: ByteArray,
    sourceOffset: Int,
    sourceStride: Int,
    target: java.nio.ByteBuffer,
    targetOffset: Int,
    targetStride: Int,
    width: Int,
    height: Int,
) {
    for (y in 0 until height) {
        val sourceRow = sourceOffset + y * sourceStride
        val targetRow = targetOffset + y * targetStride
        for (x in 0 until width) {
            target.put(targetRow + x, source[sourceRow + x])
        }
    }
}

private fun copyAlphaRows(
    source: IntArray,
    sourceOffset: Int,
    sourceStride: Int,
    target: java.nio.ByteBuffer,
    targetOffset: Int,
    targetStride: Int,
    width: Int,
    height: Int,
) {
    for (y in 0 until height) {
        val sourceRow = sourceOffset + y * sourceStride
        val targetRow = targetOffset + y * targetStride
        for (x in 0 until width) {
            target.put(targetRow + x, (source[sourceRow + x] ushr 24).toByte())
        }
    }
}
