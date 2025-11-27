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
 *
 */
package net.ccbluex.liquidbounce.integration.theme

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.copyPose
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.drawFullScreenPositionTexture
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.asView
import net.ccbluex.liquidbounce.utils.render.createUbo
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.texture.TextureSetup
import net.minecraft.util.Identifier
import java.io.Closeable
import java.util.*

sealed interface ThemeBackground : Closeable {

    /**
     * Returns false to let Minecraft render its default wallpaper.
     */
    object Minecraft : ThemeBackground {
        override fun draw(
            context: DrawContext,
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
     * @param imageId The Minecraft resource identifier for the image
     */
    class Image(private val imageId: Identifier) : ThemeBackground {

        override fun draw(
            context: DrawContext,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ): Boolean {
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                imageId,
                0, 0,
                0f, 0f,
                width, height,
                width, height
            )
            return true
        }

        override fun close() {
            mc.textureManager.destroyTexture(imageId)
        }
    }

    /**
     * Background implementation that renders using a custom shader.
     * @param pipeline the shader render pipeline
     */
    class Shader private constructor(
        private val metadata: ThemeMetadata,
        private val pipeline: RenderPipeline,
    ) : ThemeBackground {

        private val ubo = gpuDevice.createUbo(
            labelGetter = { "ThemeShaderBackground UBO - ${metadata.name}" }
        ) { float + vec2 + vec2 }

        private val uboSlice = ubo.slice()

        private var background: GpuTexture? = null
        private var backgroundView: GpuTextureView? = null

        override fun draw(
            context: DrawContext,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ): Boolean {
            val framebufferWidth = mc.window.framebufferWidth
            val framebufferHeight = mc.window.framebufferHeight

            uboSlice.writeStd140 {
                putFloat((System.currentTimeMillis() - mc.startTime) / 1000F)
                putVec2(mouseX.toFloat(), mouseY.toFloat())
                putVec2(framebufferWidth.toFloat(), framebufferHeight.toFloat())
            }

            val backgroundView = resizeIfNeeded(framebufferWidth, framebufferHeight)

            backgroundView.createRenderPass(
                { "ThemeShaderBackground Pass - ${metadata.name}" }
            ).use { pass ->
                pass.setPipeline(pipeline)
                pass.setUniform(UNIFORM_NAME, uboSlice)
                pass.drawFullScreenPositionTexture()
            }

            context.state
                .addSimpleElement(
                    TexturedQuadGuiElementRenderState(
                        RenderPipelines.GUI_TEXTURED,
                        TextureSetup.withoutGlTexture(backgroundView),
                        context.copyPose(),
                        0,
                        0,
                        width,
                        height,
                        0f,
                        1f,
                        1f,
                        0f,
                        -1,
                        null, // Always no scissor
                    )
                )

            return true
        }

        override fun close() {
            ubo.close()
            backgroundView?.close()
            background?.close()
        }

        private fun resizeIfNeeded(
            framebufferWidth: Int,
            framebufferHeight: Int,
        ): GpuTextureView {
            if (background == null ||
                background!!.getWidth(0) != framebufferWidth ||
                background!!.getHeight(0) != framebufferHeight
            ) {
                background?.close()
                background = gpuDevice.createTexture(
                    "ThemeShaderBackground Texture - ${metadata.name} ($framebufferWidth x $framebufferHeight)",
                    GpuTexture.USAGE_RENDER_ATTACHMENT,
                    TextureFormat.RGBA8, framebufferWidth, framebufferHeight,
                    1, 1,
                )
                backgroundView?.close()
                backgroundView = background!!.asView()
            }
            return backgroundView!!
        }

        companion object {
            private const val UNIFORM_NAME = "ThemeBackgroundData"

            @JvmStatic
            fun build(
                metadata: ThemeMetadata,
                background: Background,
                vertexShader: String,
                fragmentShader: String,
            ): Shader {
                val bgName = background.name.lowercase(Locale.US)
                val themeName = metadata.name.lowercase(Locale.US)

                val vshId = LiquidBounce.identifier("shader/vsh/theme-bg-$bgName")
                val fshId = LiquidBounce.identifier("shader/fsh/theme-bg-$bgName")

                val pipeline = RenderPipeline.Builder()
                    .withLocation(LiquidBounce.identifier("pipeline/theme-bg-$themeName"))
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLES)
                    .withVertexShader(vshId)
                    .withFragmentShader(fshId)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withUniform(UNIFORM_NAME, UniformType.UNIFORM_BUFFER)
                    .withoutBlend()
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()

                gpuDevice.precompilePipeline(pipeline) { id, _ ->
                    when (id) {
                        vshId -> vertexShader
                        fshId -> fragmentShader
                        else -> error("Unknown shader id: $id")
                    }
                }

                return Shader(metadata, pipeline)
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
        context: DrawContext,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ): Boolean
}
