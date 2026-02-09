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
package net.ccbluex.liquidbounce.integration.theme

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.ClientRenderPipelines.screenQuadSnippet
import net.ccbluex.liquidbounce.render.ClientRenderPipelines.withUniformBuffer
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.drawBlitOnCurrentLayer
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.asTextureSetup
import net.ccbluex.liquidbounce.utils.render.asView
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.resources.Identifier
import java.io.Closeable
import java.util.Locale

sealed interface ThemeBackground : Closeable {

    /**
     * Returns false to let Minecraft render its default wallpaper.
     */
    object Minecraft : ThemeBackground {
        override fun draw(
            context: GuiGraphics,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ): Boolean = false // Show default Minecraft wallpaper

        @Suppress("EmptyFunctionBlock")
        override fun close() { }
    }

    /**
     * Background implementation that renders a static image texture.
     * @param texture The image texture
     */
    class Image(
        private val metadata: ThemeMetadata,
        image: NativeImage,
    ) : ThemeBackground {

        private val texture = image.asTexture { "ThemeBackground/Image - ${metadata.name}" }
        private val textureSetup = texture.textureSetup

        override fun draw(
            context: GuiGraphics,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ): Boolean {
            context.drawTexQuad(
                textureSetup,
                x0 = 0f, y0 = 0f,
                x1 = width.toFloat(), y1 = height.toFloat(),
            )

            return true
        }

        override fun close() {
            texture.close()
        }
    }

    /**
     * Background implementation that renders using a custom shader.
     * @param pipeline the shader render pipeline
     */
    class Shader private constructor(
        private val metadata: ThemeMetadata,
        private val pipeline: RenderPipeline,
        private val fshId: Identifier,
        private val fragmentShader: String,
    ) : ThemeBackground {

        private val ubo = ClientUniformDefine.THEME_BACKGROUND.createRingBuffer {
            "ThemeShaderBackground UBO - ${metadata.name}"
        }

        private var background: GpuTexture? = null
        private var backgroundView: GpuTextureView? = null
        private var textureSetup: TextureSetup? = null

        override fun draw(
            context: GuiGraphics,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ): Boolean {
            val framebufferWidth = mc.window.width
            val framebufferHeight = mc.window.height

            ubo.rotate()
            val uboSlice = ubo.currentBuffer().slice()
            uboSlice.writeStd140 {
                putFloat(clientStartDurationMs / 1000F)
                putVec2(mouseX.toFloat(), mouseY.toFloat())
                putVec2(framebufferWidth.toFloat(), framebufferHeight.toFloat())
            }

            resizeIfNeeded(framebufferWidth, framebufferHeight)

            backgroundView!!.createRenderPass(
                { "ThemeShaderBackground Pass - ${metadata.name}" }
            ).use { pass ->
                pass.setPipeline(pipeline)
                pass.setUniform(ClientUniformDefine.THEME_BACKGROUND.uboName, uboSlice)
                pass.draw(0, 3)
            }

            context.drawBlitOnCurrentLayer(
                textureSetup!!,
                x0 = 0, y0 = 0,
                x1 = width, y1 = height,
                u1 = 0f, v1 = 1f,
                u2 = 1f, v2 = 0f,
            )

            return true
        }

        override fun close() {
            ubo.close()
            backgroundView?.close()
            background?.close()
        }

        override fun onResourceReload() {
            gpuDevice.precompilePipeline(pipeline) { id, _ ->
                if (id == fshId) {
                    fragmentShader
                } else {
                    error("Unknown shader id: $id")
                }
            }
        }

        private fun resizeIfNeeded(
            framebufferWidth: Int,
            framebufferHeight: Int,
        ) {
            if (background == null ||
                background!!.getWidth(0) != framebufferWidth ||
                background!!.getHeight(0) != framebufferHeight
            ) {
                background?.close()
                background = gpuDevice.createTexture(
                    "ThemeBackground/Shader - ${metadata.name} ($framebufferWidth x $framebufferHeight)",
                    GpuTexture.USAGE_RENDER_ATTACHMENT,
                    TextureFormat.RGBA8, framebufferWidth, framebufferHeight,
                    1, 1,
                )
                backgroundView?.close()
                backgroundView = background!!.asView()
                textureSetup = backgroundView!!.asTextureSetup(SAMPLER)
            }
        }

        companion object {

            @JvmStatic
            private val SAMPLER = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)

            @JvmStatic
            fun build(
                metadata: ThemeMetadata,
                background: Background,
                fragmentShader: String,
            ): Shader {
                val bgName = background.name.lowercase(Locale.US)
                val themeName = metadata.name.lowercase(Locale.US)

                val fshId = LiquidBounce.identifier("shader/fsh/theme-bg-$themeName-$bgName")

                val pipeline = RenderPipeline.Builder()
                    .withLocation(LiquidBounce.identifier("pipeline/theme-bg-$themeName"))
                    .screenQuadSnippet()
                    .withFragmentShader(fshId)
                    .withUniformBuffer(ClientUniformDefine.THEME_BACKGROUND)
                    .withoutBlend()
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()

                return Shader(metadata, pipeline, fshId, fragmentShader)
            }
        }
    }

    /**
     * Draws the background on the screen.
     * @param context The drawing context
     * @param width Screen width
     * @param height Screen height
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param delta Time delta for animations
     * @return true if background was drawn, false to use default Minecraft background
     */
    @Suppress("LongParameterList")
    fun draw(
        context: GuiGraphics,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ): Boolean

    /**
     * Called when resources are reloaded.
     */
    fun onResourceReload() {}
}
