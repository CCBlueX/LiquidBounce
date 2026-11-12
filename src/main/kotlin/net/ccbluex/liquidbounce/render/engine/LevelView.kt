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
package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.copyTo
import net.ccbluex.liquidbounce.utils.render.readBufferedImage
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.extract.LevelExtractor
import net.minecraft.world.entity.Entity
import org.joml.Vector4f
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.roundToInt

/**
 * What a client in the background sees, drawn off screen while someone keeps asking for it. A view has a renderer
 * and an extractor of its own, so the chunks of its level stay compiled between frames.
 */
class LevelView private constructor(private val frames: Frames) : AutoCloseable {

    private var width = 0
    private var height = 0

    private val camera = Camera()
    private var target: TextureTarget? = null
    private var levelRenderer: LevelRenderer? = null
    private var extractor: LevelExtractor? = null

    private var renderedLevel: ClientLevel? = null

    private fun draws(level: ClientLevel?) = level != null && level === renderedLevel

    private val readbacks = Array(READBACKS) { Readback() }
    private var nextReadback = 0

    private var renderedAt = 0L

    /**
     * Runs within the client it shows, on the render thread and outside of the window's frame. Whatever draws in
     * between, vanilla or modules, finds this view's renderer, camera and target in place of the window's.
     */
    private fun render() {
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
    private fun fit() {
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
            texture.copyTo(buffer) {
                copying = false
                if (closing) {
                    closing = false
                    buffer.close()
                    return@copyTo
                }

                buffer.readBufferedImage(width, height, image)
                encoding = true
                frames.encode(image) { encoding = false }
            }
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

    /**
     * The JPEG frames of one client's view, and when someone last asked for one.
     */
    private class Frames {

        @Volatile
        var latest: ByteArray? = null
            private set

        @Volatile
        var askedAt = System.nanoTime()

        fun encode(image: BufferedImage, done: () -> Unit) {
            encoder.execute {
                try {
                    latest = jpeg(image)
                } finally {
                    done()
                }
            }
        }

        companion object {

            private const val QUALITY = 0.75f

            private val encoder = Executors.newSingleThreadExecutor {
                Thread(it, "Client view encoder").apply { isDaemon = true }
            }

            // Only used on the encoder thread
            private val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
            private val parameters = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = QUALITY
            }
            private val output = ByteArrayOutputStream()

            private fun jpeg(image: BufferedImage): ByteArray {
                output.reset()
                ImageIO.createImageOutputStream(output).use { stream ->
                    writer.output = stream
                    writer.write(null, IIOImage(image, null, null), parameters)
                }
                return output.toByteArray()
            }

        }

    }

    companion object {

        private const val HEIGHT = 270
        private const val RENDER_DISTANCE = 4
        private const val READBACKS = 2
        private val CLEAR_COLOR = Vector4f(0f, 0f, 0f, 1f)

        private const val FRAME_INTERVAL_NANOS = 100_000_000L

        /**
         * How long one request keeps a view drawn.
         */
        private const val WATCHED_NANOS = 1_000_000_000L

        /**
         * A view outlives its last request a little, so that a page reload does not compile the chunks again.
         */
        private const val IDLE_NANOS = 5_000_000_000L

        private val watched = ConcurrentHashMap<Client, Frames>()

        // Render thread only
        private val views = Reference2ObjectOpenHashMap<Client, LevelView>()
        private val extractors = ReferenceOpenHashSet<LevelExtractor>()

        /**
         * The latest JPEG of what [client] sees, null until one is drawn. Asking keeps the view drawn for a second.
         *
         * Pages ask again and again instead of keeping a stream open: Chromium allows six connections per host,
         * and a stream for every view would leave none for the rest of the theme.
         */
        fun latestFrame(client: Client): ByteArray? {
            val frames = watched.computeIfAbsent(client) { Frames() }
            frames.askedAt = System.nanoTime()
            return frames.latest
        }

        /**
         * Draws the watched view that waited longest, if it is due. Only one per frame: the uniforms of the renderers
         * live in rings of three buffers, which one frame must not go round, and the window takes one of them.
         */
        internal fun renderNext() {
            val now = System.nanoTime()
            var next: Client? = null
            var nextAt = Long.MAX_VALUE
            for ((client, frames) in watched) {
                val asked = now - frames.askedAt
                if (asked >= IDLE_NANOS) {
                    watched.remove(client, frames)
                    drop(client)
                    continue
                }
                if (asked >= WATCHED_NANOS || client.isActive) {
                    continue
                }

                val renderedAt = views[client]?.renderedAt ?: 0L
                if (now - renderedAt >= FRAME_INTERVAL_NANOS && renderedAt < nextAt) {
                    next = client
                    nextAt = renderedAt
                }
            }

            val client = next ?: return
            val view = views.getOrPut(client) { LevelView(watched[client] ?: return) }
            ClientContext.within(client) {
                view.fit()
                view.extractor?.let(extractors::add)
                view.render()
            }
        }

        /**
         * Drops the view of [client], for when it takes over the window.
         */
        internal fun drop(client: Client) {
            val view = views.remove(client) ?: return
            view.extractor?.let(extractors::remove)
            view.close()
        }

        /**
         * Drops the view and frames of [client], which is gone.
         */
        internal fun forget(client: Client) {
            drop(client)
            watched.remove(client)
        }

        @JvmStatic
        fun closeAll() {
            views.keys.toList().forEach(::drop)
        }

        @JvmStatic
        fun isView(extractor: LevelExtractor) = extractor in extractors

        /**
         * The extractor that should hear about a change reported to [extractor]. Levels of background clients report
         * to the window's, which draws the active client only, so their changes go to their view if it already draws
         * that level. A view that does not draw it yet reads the whole level once it starts to.
         */
        @JvmStatic
        fun receiverOf(extractor: LevelExtractor): LevelExtractor? =
            if (!ClientContext.isBackground || extractor in extractors) {
                extractor
            } else {
                views[ClientContext.loaded]?.takeIf { it.draws(mc.level) }?.extractor
            }

        /**
         * Puts the renderer of the loaded client's view in place of the window's while the view hears of a change, and
         * returns the window's. Sodium finds the renderer of an extractor through [Minecraft.levelRenderer].
         */
        @JvmStatic
        fun lendRenderer(): LevelRenderer {
            val window = mc.levelRenderer
            views[ClientContext.loaded]?.levelRenderer?.let { mc.levelRenderer = it }
            return window
        }

    }

}
