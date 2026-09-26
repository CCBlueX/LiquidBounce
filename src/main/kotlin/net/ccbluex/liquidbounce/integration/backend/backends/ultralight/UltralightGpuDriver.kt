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
package net.ccbluex.liquidbounce.integration.backend.backends.ultralight

import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.commands.CommandEncoder
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.renderpearl.api.pipeline.IndexType
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.ccbluex.liquidbounce.render.ClientRenderPipelines.ULTRALIGHT
import net.ccbluex.liquidbounce.render.setPipeline
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.janrupf.ujr.api.bitmap.UlBitmapFormat
import net.janrupf.ujr.api.bitmap.UltralightBitmap
import net.janrupf.ujr.api.gpu.UlCommand
import net.janrupf.ujr.api.gpu.UlCommandList
import net.janrupf.ujr.api.gpu.UlCommandType
import net.janrupf.ujr.api.gpu.UlGPUState
import net.janrupf.ujr.api.gpu.UlRenderBuffer
import net.janrupf.ujr.api.gpu.UlShaderType
import net.janrupf.ujr.api.gpu.UlVertexBufferFormat
import net.janrupf.ujr.api.gpu.UltralightGPUDriver
import net.minecraft.client.gui.render.TextureSetup
import org.joml.Matrix4f
import org.joml.Vector4f
import java.nio.ByteBuffer
import java.util.Optional
import java.util.function.Supplier

/**
 * Size of the `UltralightState` uniform block of the shaders.
 */
private const val UNIFORMS_SIZE = 768L

private val PASS_LABEL = Supplier { "Ultralight" }
private val VERTICES_LABEL = Supplier { "Ultralight vertices" }
private val INDICES_LABEL = Supplier { "Ultralight indices" }
private val TRANSPARENT = Vector4f(0f)

/**
 * Draws the pages of Ultralight with the renderer of the game, so they never leave the GPU.
 *
 * Ultralight calls the driver on the render thread during [net.janrupf.ujr.api.UltralightRenderer.render], and the
 * command lists are drawn right away. Colour textures keep the BGRA pixels of Ultralight, which the shaders swizzle.
 *
 * No exception may leave the driver, Ultralight's renderer can't draw anymore after one unwound through it.
 */
@Suppress("TooManyFunctions")
internal class UltralightGpuDriver : UltralightGPUDriver, AutoCloseable {

    private class Texture(val texture: GpuTexture, val view: GpuTextureView, val isRenderTarget: Boolean) {

        /**
         * Whether the texture holds anything yet, render targets are undefined until Ultralight draws into them.
         */
        var isReady = !isRenderTarget

        fun close() {
            view.close()
            texture.close()
        }

    }

    private class Geometry(var vertices: GpuBuffer, var indices: GpuBuffer) {

        fun close() {
            vertices.close()
            indices.close()
        }

    }

    private val logger = clientLogger("Ultralight")
    private var hasFailed = false

    private val textures = Int2ObjectOpenHashMap<Texture>()
    private val renderBufferTextures = Int2IntOpenHashMap()
    private val geometries = Int2ObjectOpenHashMap<Geometry>()

    private var lastTextureId = 0
    private var lastRenderBufferId = 0
    private var lastGeometryId = 0

    /**
     * Bound to the samplers a draw doesn't use, as the pipelines need all of them.
     */
    private val emptyTexture = createEmptyTexture()

    private val sampler
        get() = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)

    private val transform = Matrix4f()
    private val projection = Matrix4f()
    private val matrixValues = FloatArray(16)

    /**
     * Finds the texture Ultralight drew into, once it holds a page.
     */
    fun textureSetup(textureId: Int): TextureSetup? {
        val texture = textures[textureId]?.takeIf { it.isReady } ?: return null
        return TextureSetup.singleTexture(texture.view, sampler)
    }

    override fun beginSynchronize() {
        // Everything is drawn in order on the render thread
    }

    override fun endSynchronize() {
        // Everything is drawn in order on the render thread
    }

    override fun nextTextureId() = ++lastTextureId

    override fun createTexture(textureId: Int, bitmap: UltralightBitmap) = guarded("create a texture") {
        textures.put(textureId, createTexture(bitmap))?.close()
    }

    override fun updateTexture(textureId: Int, bitmap: UltralightBitmap) = guarded("update a texture") {
        val texture = textures[textureId]
        if (texture == null || texture.texture.getWidth(0) != bitmap.width().toInt() ||
            texture.texture.getHeight(0) != bitmap.height().toInt() ||
            texture.texture.format != textureFormat(bitmap)) {
            textures.put(textureId, createTexture(bitmap))?.close()
            return@guarded
        }

        if (!bitmap.isEmpty) {
            upload(texture.texture, bitmap)
        }
    }

    override fun destroyTexture(textureId: Int) = guarded("destroy a texture") {
        textures.remove(textureId)?.close()
    }

    override fun nextRenderBufferId() = ++lastRenderBufferId

    override fun createRenderBuffer(renderBufferId: Int, renderBuffer: UlRenderBuffer) {
        renderBufferTextures.put(renderBufferId, renderBuffer.textureId())
    }

    override fun destroyRenderBuffer(renderBufferId: Int) {
        renderBufferTextures.remove(renderBufferId)
    }

    override fun nextGeometryId() = ++lastGeometryId

    override fun createGeometry(
        geometryId: Int,
        format: UlVertexBufferFormat,
        vertices: ByteBuffer,
        indices: ByteBuffer
    ) = guarded("create geometry") {
        val geometry = Geometry(
            gpuDevice.createBuffer(VERTICES_LABEL, GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, vertices),
            gpuDevice.createBuffer(INDICES_LABEL, GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, indices)
        )
        geometries.put(geometryId, geometry)?.close()
    }

    override fun updateGeometry(
        geometryId: Int,
        format: UlVertexBufferFormat,
        vertices: ByteBuffer,
        indices: ByteBuffer
    ) = guarded("update geometry") {
        val geometry = geometries[geometryId]
        if (geometry == null) {
            createGeometry(geometryId, format, vertices, indices)
            return@guarded
        }

        geometry.vertices = write(geometry.vertices, VERTICES_LABEL, GpuBuffer.USAGE_VERTEX, vertices)
        geometry.indices = write(geometry.indices, INDICES_LABEL, GpuBuffer.USAGE_INDEX, indices)
    }

    override fun destroyGeometry(geometryId: Int) = guarded("destroy geometry") {
        geometries.remove(geometryId)?.close()
    }

    override fun updateCommandList(commands: UlCommandList) = guarded("draw a page") {
        val encoder = gpuDevice.createCommandEncoder()

        // Written before any pass begins
        val uniforms = arrayOfNulls<GpuBufferSlice>(commands.size())
        for (i in 0 until commands.size()) {
            val command = commands.get(i)
            val target = renderTarget(command.state()) ?: continue

            if (command.type() == UlCommandType.DRAW_GEOMETRY) {
                uniforms[i] = writeUniforms(encoder, command.state(), target.texture)
            }
        }

        var pass: RenderPass? = null
        var passRenderBuffer = 0

        try {
            for (i in 0 until commands.size()) {
                val command = commands.get(i)
                val state = command.state()
                val target = renderTarget(state) ?: continue

                when (command.type()) {
                    UlCommandType.CLEAR_RENDER_BUFFER -> {
                        pass?.close()
                        pass = null

                        encoder.clearColorTexture(target.texture, TRANSPARENT)
                        target.isReady = true
                    }

                    UlCommandType.DRAW_GEOMETRY -> {
                        val geometry = geometries[command.geometryId()] ?: continue

                        if (pass == null || passRenderBuffer != state.renderBufferId()) {
                            pass?.close()
                            pass = encoder.createRenderPass(PASS_LABEL, target.view, Optional.empty())
                            passRenderBuffer = state.renderBufferId()
                        }

                        draw(pass, command, target.texture, geometry, uniforms[i]!!)
                        target.isReady = true
                    }
                }
            }
        } finally {
            pass?.close()
        }
    }

    private fun draw(
        pass: RenderPass,
        command: UlCommand,
        target: GpuTexture,
        geometry: Geometry,
        uniforms: GpuBufferSlice
    ) {
        val state = command.state()

        if (state.enableScissor()) {
            val rect = state.scissorRect()
            val width = target.getWidth(0)
            val height = target.getHeight(0)
            val left = rect.left.coerceIn(0, width)
            val top = rect.top.coerceIn(0, height)
            val right = rect.right.coerceIn(left, width)
            val bottom = rect.bottom.coerceIn(top, height)

            if (right == left || bottom == top) {
                return
            }

            pass.enableScissor(left, top, right - left, bottom - top)
        } else {
            pass.disableScissor()
        }

        val blend = state.enableBlend()
        when (state.shaderType()) {
            UlShaderType.FILL -> {
                pass.setPipeline(if (blend) ULTRALIGHT.FILL_BLENDED else ULTRALIGHT.FILL)
                pass.setUniform("Texture1", textureView(state.texture1Id()), sampler)
                pass.setUniform("Texture2", textureView(state.texture2Id()), sampler)
            }

            UlShaderType.FILL_PATH ->
                pass.setPipeline(if (blend) ULTRALIGHT.FILL_PATH_BLENDED else ULTRALIGHT.FILL_PATH)
        }

        pass.setUniform("UltralightState", uniforms)
        pass.setVertexBuffer(0, geometry.vertices.slice())
        pass.setIndexBuffer(geometry.indices, IndexType.INT)
        pass.drawIndexed(command.indicesCount(), 1, command.indicesOffset(), 0, 0)
    }

    private fun writeUniforms(encoder: CommandEncoder, state: UlGPUState, target: GpuTexture): GpuBufferSlice {
        val alignment = gpuDevice.deviceInfo.limits().minUniformOffsetAlignment().toLong()
        val memory = encoder.transientMemory().allocateGpuMapped(UNIFORMS_SIZE, alignment, GpuBuffer.USAGE_UNIFORM)

        memory.use {
            val builder = Std140Builder.intoBuffer(it.data())
                .putVec4(0f, state.viewportWidth().toFloat(), state.viewportHeight().toFloat(), 1f)
                .putMat4f(transform(state, target))

            for (i in 0 until UlGPUState.UNIFORM_SCALARS step 4) {
                builder.putVec4(
                    state.uniformScalar(i),
                    state.uniformScalar(i + 1),
                    state.uniformScalar(i + 2),
                    state.uniformScalar(i + 3)
                )
            }

            for (i in 0 until UlGPUState.UNIFORM_VECTORS) {
                builder.putVec4(
                    state.uniformVector(i, 0),
                    state.uniformVector(i, 1),
                    state.uniformVector(i, 2),
                    state.uniformVector(i, 3)
                )
            }

            builder.putInt(state.clipSize())
            builder.align(16)

            for (clip in 0 until UlGPUState.MAX_CLIPS) {
                for (i in 0 until 16) {
                    matrixValues[i] = state.clip(clip, i)
                }
                builder.putMat4f(transform.set(matrixValues))
            }
        }

        return memory.slice()
    }

    /**
     * Projects the page onto the render target, with its top in the first row of the texture.
     *
     * The render pass covers the whole texture, which starts at the origin of Ultralight's viewport, so the projection
     * works in texels of the texture instead.
     */
    private fun transform(state: UlGPUState, target: GpuTexture): Matrix4f {
        for (i in 0 until 16) {
            matrixValues[i] = state.transform(i)
        }

        val width = target.getWidth(0).toFloat()
        val height = target.getHeight(0).toFloat()

        // Depth is unused and stays at zero, which lies within the clip space of every backend
        projection.set(
            2f / width, 0f, 0f, 0f,
            0f, 2f / height, 0f, 0f,
            0f, 0f, 0f, 0f,
            -1f, -1f, 0f, 1f
        )
        return projection.mul(transform.set(matrixValues))
    }

    private fun renderTarget(state: UlGPUState): Texture? {
        val textureId = renderBufferTextures.get(state.renderBufferId())
        return textures[textureId]
    }

    private fun textureView(textureId: Int): GpuTextureView = (textures[textureId] ?: emptyTexture).view

    private fun createTexture(bitmap: UltralightBitmap): Texture {
        val isRenderTarget = bitmap.isEmpty
        var usage = GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING
        if (isRenderTarget) {
            usage = usage or GpuTexture.USAGE_RENDER_ATTACHMENT
        }

        val gpuTexture = gpuDevice.createTexture(
            "Ultralight texture",
            usage,
            textureFormat(bitmap),
            bitmap.width().toInt(),
            bitmap.height().toInt(),
            1,
            1
        )
        val texture = Texture(gpuTexture, gpuDevice.createTextureView(gpuTexture), isRenderTarget)

        if (!isRenderTarget) {
            upload(gpuTexture, bitmap)
        }

        return texture
    }

    private fun createEmptyTexture(): Texture {
        val gpuTexture = gpuDevice.createTexture(
            "Ultralight empty texture",
            GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_RENDER_ATTACHMENT,
            GpuFormat.RGBA8_UNORM,
            1,
            1,
            1,
            1
        )
        gpuDevice.createCommandEncoder().clearColorTexture(gpuTexture, TRANSPARENT)

        return Texture(gpuTexture, gpuDevice.createTextureView(gpuTexture), false)
    }

    /**
     * Uploads the pixels of a bitmap, which is the only way anything reaches the GPU: images and glyphs.
     */
    private fun upload(texture: GpuTexture, bitmap: UltralightBitmap) {
        val width = texture.getWidth(0)
        val height = texture.getHeight(0)
        val rowPixels = (bitmap.rowBytes() / bitmap.bpp()).toInt()

        bitmap.lockPixels().use { pixels ->
            val encoder = gpuDevice.createCommandEncoder()
            val staging = encoder.transientMemory()
                .uploadStaging(pixels.asByteBuffer(), 4L, GpuBuffer.USAGE_COPY_SRC)

            encoder.copyBufferToTexture(staging, 0, 0, rowPixels, height, texture, 0, 0, width, height, 0, 0)
        }
    }

    private fun write(buffer: GpuBuffer, label: Supplier<String>, usage: Int, data: ByteBuffer): GpuBuffer {
        if (data.remaining() <= buffer.size()) {
            gpuDevice.createCommandEncoder().writeToBuffer(buffer.slice(0, data.remaining().toLong()), data)
            return buffer
        }

        buffer.close()
        return gpuDevice.createBuffer(label, usage or GpuBuffer.USAGE_COPY_DST, data)
    }

    private fun textureFormat(bitmap: UltralightBitmap) = when (bitmap.format()) {
        UlBitmapFormat.A8_UNORM -> GpuFormat.R8_UNORM
        else -> GpuFormat.RGBA8_UNORM
    }

    override fun close() {
        textures.values.forEach(Texture::close)
        textures.clear()
        geometries.values.forEach(Geometry::close)
        geometries.clear()
        renderBufferTextures.clear()

        emptyTexture.close()
    }

    private inline fun guarded(action: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // Reported once, it would repeat every frame
            if (!hasFailed) {
                hasFailed = true
                logger.error("Failed to $action for Ultralight", e)
            }
        }
    }

}
