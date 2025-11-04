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

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.drawFullScreenPositionTexture
import net.ccbluex.liquidbounce.render.newRenderPass
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gl.UniformType
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import java.io.Closeable
import java.util.Locale

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
                RenderLayer::getGuiTextured,
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
        private val pipeline: RenderPipeline,
    ) : ThemeBackground {

        override fun draw(
            context: DrawContext,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            delta: Float
        ): Boolean {
            newRenderPass().use { pass ->
                pass.setPipeline(pipeline)
                pass.setUniform(UNIFORM_TIME, (System.currentTimeMillis() - mc.startTime) / 1000F)
                pass.setUniform(UNIFORM_MOUSE, mouseX.toFloat(), mouseY.toFloat())
                pass.setUniform(
                    UNIFORM_RESOLUTION,
                    mc.window.framebufferWidth.toFloat(),
                    mc.window.framebufferHeight.toFloat(),
                )

                pass.drawFullScreenPositionTexture()
            }
            return true
        }

        @Suppress("EmptyFunctionBlock")
        override fun close() {
        }

        companion object {
            private const val UNIFORM_TIME = "time"
            private const val UNIFORM_MOUSE = "mouse"
            private const val UNIFORM_RESOLUTION = "resolution"

            @JvmStatic
            fun build(
                metadata: ThemeMetadata,
                background: Background,
                vertexShader: String,
                fragmentShader: String,
            ): Shader {
                val vshId = LiquidBounce.identifier("vsh/${background.name.lowercase(Locale.US)}")
                val fshId = LiquidBounce.identifier("fsh/${background.name.lowercase(Locale.US)}")

                val pipeline = RenderPipeline.Builder()
                    .withLocation(LiquidBounce.identifier("theme-bg-${metadata.name.lowercase(Locale.US)}"))
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLES)
                    .withVertexShader(vshId)
                    .withFragmentShader(fshId)
                    .withUniform(UNIFORM_TIME, UniformType.FLOAT)
                    .withUniform(UNIFORM_MOUSE, UniformType.VEC2)
                    .withUniform(UNIFORM_RESOLUTION, UniformType.VEC2)
                    .withoutBlend()
                    .build()

                gpuDevice.precompilePipeline(pipeline) { id, _ ->
                    when (id) {
                        vshId -> vertexShader
                        fshId -> fragmentShader
                        else -> error("Unknown shader id: $id")
                    }
                }

                return Shader(pipeline)
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
