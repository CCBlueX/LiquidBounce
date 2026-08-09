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

package net.ccbluex.liquidbounce.render.atlas

import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.copyTo
import net.ccbluex.liquidbounce.utils.render.readFully
import net.ccbluex.liquidbounce.utils.render.withOutputTextureOverride
import net.minecraft.client.renderer.Projection
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.util.Util
import net.minecraft.util.Mth
import okio.Buffer
import org.apache.commons.lang3.function.Consumers
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.LazyThreadSafetyMode.NONE

internal abstract class AbstractAtlasRenderer<A : Any>(
    private val label: String,
) : MinecraftShortcuts {

    protected abstract val tileSize: Int
    protected abstract val tileCount: Int

    private val tilesPerRow: Int
        get() = Mth.smallestSquareSide(tileCount)

    protected val textureSize: Int
        get() = tileSize * tilesPerRow

    private val framebufferLazy = lazy(NONE) {
        TextureTarget(
            "$label atlas framebuffer",
            textureSize,
            textureSize,
            true,
            GpuFormat.RGBA8_UNORM,
        )
    }
    protected val framebuffer by framebufferLazy

    private val submitNodeStorageLazy = lazy(NONE, ::SubmitNodeStorage)
    protected val submitNodeStorage by submitNodeStorageLazy

    private val featureRenderDispatcherLazy = lazy(NONE) {
        FeatureRenderDispatcher(
            mc.gameRenderer.renderBuffers,
            mc.modelManager,
            mc.atlasManager,
            mc.font,
            mc.gameRenderer.gameRenderState(),
        )
    }
    protected val featureRenderDispatcher by featureRenderDispatcherLazy

    protected val poseStack = PoseStack()
    protected val projection = Projection()

    private val projectionMatrixBufferLazy = lazy(NONE) {
        ProjectionMatrixBuffer("$label atlas")
    }
    protected val projectionMatrixBuffer by projectionMatrixBufferLazy

    private val closed = AtomicBoolean()

    abstract fun render(): CompletableFuture<A>

    protected fun tileRect(index: Int) = Rect2i(
        (index % tilesPerRow) * tileSize,
        (index / tilesPerRow) * tileSize,
        tileSize,
        tileSize,
    )

    protected fun withTile(rect: Rect2i, block: PoseStack.() -> Unit) {
        poseStack.pushPose()
        try {
            RenderSystem.enableScissorForRenderTypeDraws(
                rect.x,
                textureSize - rect.y - rect.height,
                rect.width,
                rect.height,
            )
            try {
                poseStack.block()
            } finally {
                RenderSystem.disableScissorForRenderTypeDraws()
            }
        } finally {
            poseStack.popPose()
        }
    }

    protected fun <T> withAtlasTarget(block: () -> T): T {
        framebuffer.clearColorAndDepth()
        RenderSystem.backupProjectionMatrix()
        try {
            projection.setupOrtho(
                -1000.0F,
                1000.0F,
                textureSize.toFloat(),
                textureSize.toFloat(),
                true,
            )
            RenderSystem.setProjectionMatrix(
                projectionMatrixBuffer.getBuffer(projection),
                ProjectionType.ORTHOGRAPHIC,
            )
            return withOutputTextureOverride(
                framebuffer.colorTextureView,
                framebuffer.depthTextureView,
                block,
            )
        } finally {
            RenderSystem.restoreProjectionMatrix()
        }
    }

    protected fun readbackAsync(
        buildAtlas: (ByteBuffer, CompletableFuture<A>) -> A,
    ): CompletableFuture<A> {
        val colorTexture = requireNotNull(framebuffer.colorTexture) {
            "$label atlas framebuffer has no color texture"
        }
        val readbackBuffer = gpuDevice.createBuffer(
            { "$label atlas readback" },
            GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
            textureSize.toLong() * textureSize * GpuFormat.RGBA8_UNORM.blockSize(),
        )
        val result = CompletableFuture<A>()

        try {
            colorTexture.copyTo(readbackBuffer) {
                processReadback(readbackBuffer, result, buildAtlas)
            }
        } catch (throwable: Throwable) {
            closeReadbackResources(readbackBuffer)?.let(throwable::addSuppressed)
            completeExceptionally(result, throwable)
        }

        return result
    }

    protected fun <K> encodePngTiles(
        atlasPixels: ByteBuffer,
        tileRects: Map<K, Rect2i>,
        result: CompletableFuture<A>,
    ): Map<K, ByteArray> = buildMap(tileRects.size) {
        NativeImage(tileSize, tileSize, false).use { tileImage ->
            val buffer = Buffer()
            for ((key, rect) in tileRects) {
                if (result.isCancelled) {
                    throw CancellationException("$label atlas generation was cancelled")
                }

                atlasPixels.copyRectTo(tileImage, rect, textureSize)
                check(tileImage.writeToChannel(buffer)) { "Failed to encode $label atlas tile $key" }
                this[key] = buffer.readByteArray()
            }
        }
    }

    private fun processReadback(
        readbackBuffer: GpuBuffer,
        result: CompletableFuture<A>,
        buildAtlas: (ByteBuffer, CompletableFuture<A>) -> A,
    ) {
        val atlasPixels = if (result.isCancelled) {
            null
        } else {
            try {
                readbackBuffer.readFully()
            } catch (throwable: Throwable) {
                closeReadbackResources(readbackBuffer)?.let(throwable::addSuppressed)
                completeExceptionally(result, throwable)
                return
            }
        }

        val closeFailure = closeReadbackResources(readbackBuffer)
        if (closeFailure != null) {
            MemoryUtil.memFree(atlasPixels)
            completeExceptionally(result, closeFailure)
            return
        }
        if (atlasPixels == null) {
            return
        }

        encodeAsync(atlasPixels, result, buildAtlas)
    }

    private fun encodeAsync(
        atlasPixels: ByteBuffer,
        result: CompletableFuture<A>,
        buildAtlas: (ByteBuffer, CompletableFuture<A>) -> A,
    ) {
        try {
            Util.backgroundExecutor().execute {
                try {
                    if (!result.isCancelled) {
                        val atlas = buildAtlas(atlasPixels, result)
                        if (!result.isCancelled) {
                            result.complete(atlas)
                        }
                    }
                } catch (throwable: Throwable) {
                    completeExceptionally(result, throwable)
                } finally {
                    MemoryUtil.memFree(atlasPixels)
                }
            }
        } catch (throwable: Throwable) {
            MemoryUtil.memFree(atlasPixels)
            completeExceptionally(result, throwable)
        }
    }

    private fun closeReadbackResources(readbackBuffer: GpuBuffer): Throwable? {
        var failure: Throwable? = null
        try {
            readbackBuffer.close()
        } catch (throwable: Throwable) {
            failure = throwable
        }

        try {
            close()
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }
        return failure
    }

    private fun completeExceptionally(result: CompletableFuture<*>, throwable: Throwable) {
        if (throwable !is CancellationException) {
            logger.error("Failed to load $label atlas", throwable)
        }
        result.completeExceptionally(throwable)
    }

    protected fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        if (projectionMatrixBufferLazy.isInitialized()) {
            projectionMatrixBuffer.close()
        }
        if (framebufferLazy.isInitialized()) {
            framebuffer.destroyBuffers()
        }
        if (submitNodeStorageLazy.isInitialized()) {
            submitNodeStorage.drainPhases(Consumers.nop())
        }
        if (featureRenderDispatcherLazy.isInitialized()) {
            featureRenderDispatcher.close()
        }
    }
}

private fun ByteBuffer.copyRectTo(target: NativeImage, rect: Rect2i, atlasSize: Int) {
    require(target.format() == NativeImage.Format.RGBA)
    require(rect.width == target.width && rect.height == target.height)
    require(
        rect.x >= 0 && rect.y >= 0 &&
            rect.x + rect.width <= atlasSize && rect.y + rect.height <= atlasSize
    )

    val bytesPerPixel = NativeImage.Format.RGBA.components()
    val rowBytes = rect.width * bytesPerPixel.toLong()
    val sourcePixels = MemoryUtil.memAddress(this)
    for (row in 0 until rect.height) {
        // GPU readback rows are bottom-up while atlas rectangles use top-down coordinates.
        val sourceY = atlasSize - rect.y - row - 1
        val sourceOffset = (sourceY * atlasSize + rect.x) * bytesPerPixel.toLong()
        val targetOffset = row * rowBytes
        MemoryUtil.memCopy(sourcePixels + sourceOffset, target.pointer + targetOffset, rowBytes)
    }
}
