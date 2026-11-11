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
package net.ccbluex.liquidbounce.features.client.view

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.extract.LevelExtractor
import net.minecraft.world.entity.Entity
import org.joml.Vector4f
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.roundToInt

/**
 * What a background client sees, drawn off screen. It has a renderer and extractor of its own, so the chunks
 * of its level stay compiled between frames.
 */
internal class ClientView(private val client: Client, private val frames: ClientViewFrames) : AutoCloseable {

    private var width = 0
    private var height = 0

    private val camera = Camera()
    private var target: TextureTarget? = null
    private var levelRenderer: LevelRenderer? = null

    var extractor: LevelExtractor? = null
        private set

    private var renderedLevel: ClientLevel? = null
    private val readbacks = Array(READBACKS) { Readback() }
    private var nextReadback = 0

    var renderedAt = 0L
        private set

    /**
     * Runs within [client] on the render thread, outside of the window's frame. Whatever draws in between,
     * vanilla or modules, finds this view's renderer, camera and target in place of the window's.
     */
    fun render() {
        val level = mc.level ?: return
        val player = mc.player ?: return

        val gameRenderer = mc.gameRenderer
        val windowTarget = gameRenderer.mainRenderTarget
        val windowCamera = gameRenderer.mainCamera
        val windowRenderer = mc.levelRenderer
        val windowExtractor = mc.levelExtractor
        val serverRenderDistance = mc.options.serverRenderDistance
        val levelRenderer = levelRenderer!!

        gameRenderer.mainRenderTarget = target!!
        gameRenderer.mainCamera = camera
        mc.levelRenderer = levelRenderer
        mc.levelExtractor = extractor!!
        mc.options.serverRenderDistance = if (serverRenderDistance in 1..RENDER_DISTANCE) {
            serverRenderDistance
        } else {
            RENDER_DISTANCE
        }
        try {
            draw(level, mc.cameraEntity ?: player)
        } finally {
            // Both renderers keep their uniforms in ring buffers that move on once per frame.
            gameRenderer.fogRenderer.endFrame()
            levelRenderer.endFrame()

            gameRenderer.mainRenderTarget = windowTarget
            gameRenderer.mainCamera = windowCamera
            mc.levelRenderer = windowRenderer
            mc.levelExtractor = windowExtractor
            mc.options.serverRenderDistance = serverRenderDistance
            renderedAt = System.nanoTime()
        }
    }

    private fun draw(level: ClientLevel, cameraEntity: Entity) {
        val gameRenderer = mc.gameRenderer
        val target = target!!
        val extractor = extractor!!
        if (renderedLevel !== level) {
            camera.setLevel(level)
            extractor.setLevel(level)
            renderedLevel = level
        }
        if (camera.entity() !== cameraEntity) {
            camera.setEntity(cameraEntity)
        }

        val deltaTracker = mc.deltaTracker
        val partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false)
        val state = gameRenderer.gameRenderState()
        camera.tick()
        camera.update(deltaTracker)
        gameRenderer.lightmapRenderStateExtractor.extract(state.lightmapRenderState, 1f)
        gameRenderer.extractCamera(deltaTracker, partialTicks)
        extractor.extract(deltaTracker, camera, partialTicks)

        gameRenderer.globalSettingsUniform.update(
            width,
            height,
            state.optionsRenderState.glintStrength,
            state.levelRenderState.gameTime,
            state.levelRenderState.worldPartialTicks,
            0,
            state.levelRenderState.cameraRenderState.pos,
            false
        )
        gameRenderer.lightmap.render(state.lightmapRenderState)
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(target.colorTexture!!, CLEAR_COLOR, target.depthTexture!!, 0.0)
        gameRenderer.renderLevel()
        levelRenderer!!.blitEntityOutline()
        readBack(target)
    }

    /**
     * Matches the aspect ratio of the window, building the renderer the first time.
     */
    fun fit() {
        val window = mc.window
        val height = HEIGHT
        val width = (height * window.width.toFloat() / window.height.coerceAtLeast(1)).roundToInt() and 1.inv()
        if (width == this.width && height == this.height && target != null) {
            return
        }

        this.width = width
        this.height = height
        readbacks.forEach(Readback::close)

        val target = target
        if (target == null) {
            this.target = TextureTarget("Client view", width, height, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT)
            val levelRenderer = LevelRenderer(
                mc.entityRenderDispatcher,
                mc.blockEntityRenderDispatcher,
                mc.modelManager,
                mc.textureManager,
                mc.atlasManager,
                mc.shaderManager,
                mc.gameRenderer,
                width,
                height
            )
            this.levelRenderer = levelRenderer
            extractor = LevelExtractor(mc, mc.gameRenderer.gameRenderState().levelRenderState, levelRenderer)
        } else {
            target.resize(width, height)
            levelRenderer!!.resize(width, height)
        }
    }

    private fun readBack(target: TextureTarget) {
        val readback = readbacks[nextReadback]
        if (readback.busy) {
            return
        }
        nextReadback = (nextReadback + 1) % READBACKS
        readback.copy(target, width, height)
    }

    override fun close() {
        readbacks.forEach(Readback::close)
        levelRenderer?.close()
        target?.destroyBuffers()
        target = null
        levelRenderer = null
        extractor = null
    }

    /**
     * One frame on its way from the GPU to a JPEG. The buffer is only in use while [copying], the image while
     * [encoding].
     */
    private inner class Readback : AutoCloseable {

        private var copying = false

        @Volatile
        private var encoding = false

        val busy get() = copying || encoding

        private var buffer: GpuBuffer? = null
        private var image: BufferedImage? = null

        /**
         * Set when the buffer is closed while a copy into it is still on its way, the copy closes it then.
         */
        private var closing = false

        fun copy(target: TextureTarget, width: Int, height: Int) {
            val texture = target.colorTexture ?: return
            val buffer = buffer ?: RenderSystem.getDevice().createBuffer(
                { "Client view readback" },
                GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
                width.toLong() * height * texture.format.blockSize()
            ).also { buffer = it }
            val image = image?.takeIf { it.width == width && it.height == height }
                ?: BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { image = it }

            copying = true
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, {
                copying = false
                if (closing) {
                    closing = false
                    buffer.close()
                    return@copyTextureToBuffer
                }

                buffer.map(true, false).use { view ->
                    val data = view.data()
                    val pixels = (image.raster.dataBuffer as DataBufferInt).data
                    // Rows come bottom up, each pixel as ABGR
                    for (y in 0 until height) {
                        val row = (height - 1 - y) * width
                        for (x in 0 until width) {
                            val abgr = data.getInt((x + y * width) * Int.SIZE_BYTES)
                            pixels[row + x] = (abgr and 0xFF shl 16) or (abgr and 0xFF00) or (abgr shr 16 and 0xFF)
                        }
                    }
                }
                encoding = true
                frames.encode(image) { encoding = false }
            }, 0)
        }

        override fun close() {
            if (copying) {
                closing = true
            } else {
                buffer?.close()
            }
            buffer = null
        }

    }

    private companion object {
        const val HEIGHT = 180
        const val RENDER_DISTANCE = 4
        const val READBACKS = 2
        val CLEAR_COLOR = Vector4f(0f, 0f, 0f, 1f)
    }

}
