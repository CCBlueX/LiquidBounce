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
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.withOutputTextureOverride
import net.minecraft.client.renderer.Projection
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import org.apache.commons.lang3.function.Consumers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.LazyThreadSafetyMode.NONE

internal abstract class AbstractAtlasRenderer<A : Any>(
    private val label: String,
) : MinecraftShortcuts {

    protected abstract val tileSize: Int
    protected abstract val tilesPerRow: Int

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
