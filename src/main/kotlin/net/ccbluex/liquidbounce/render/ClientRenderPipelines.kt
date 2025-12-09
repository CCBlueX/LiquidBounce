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
 */

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier

object ClientRenderPipelines {

    private val renderPipelines = Object2ObjectOpenHashMap<Identifier, RenderPipeline>()

    /**
     * Blend mode for JCEF compatible blending.
     */
    private val JCEF_COMPATIBLE_BLEND = BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)

    private val COVERING_BLEND = BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA)

    @Suppress("unused")
    private val OLD_DEFAULT_BLEND = BlendFunction(
        SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
        SourceFactor.ONE, DestFactor.ZERO,
    )

    private inline fun newPipeline(
        name: String,
        builderAction: RenderPipeline.Builder.() -> Unit,
    ): RenderPipeline {
        val id = LiquidBounce.identifier("pipeline/$name")
        return RenderPipeline.Builder()
            .withLocation(id)
            .apply(builderAction)
            .build().also { r ->
                renderPipelines.put(id, r)?.let { error("Duplicated render pipeline: $id") }
            }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun RenderPipeline.Builder.bgraPosTexColorQuads() = apply {
        withVertexShader("core/position_tex_color")
        withFragmentShader(ClientShaders.BGRA_FSH_ID)
        withSampler("Sampler0")
        withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
        withSnippet(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun RenderPipeline.Builder.forWorldRender() = apply {
        withCull(false)
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        withBlend(COVERING_BLEND)
    }

    object JCEF {
        @JvmField
        val SMOOTH_TEXTURE = newPipeline("jcef/smooth_texture") {
            withSnippet(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            withBlend(BlendFunction.TRANSLUCENT)
            withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        }

        @JvmField
        val BLURRED_TEXTURE = newPipeline("jcef/blurred_texture") {
            withSnippet(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        }

        @JvmField
        val BGRA_TEXTURE = newPipeline("jcef/bgra_texture") {
            bgraPosTexColorQuads()
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        }

        @JvmField
        val BGRA_BLURRED_TEXTURE = newPipeline("jcef/bgra_blurred_texture") {
            bgraPosTexColorQuads()
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        }

        @JvmField
        val Blit = newPipeline("jcef_blit") {
            withLocation("pipeline/entity_outline_blit")
            withVertexShader("core/blit_screen")
            withFragmentShader("core/blit_screen")
            withSampler("InSampler")
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthWrite(false)
            withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            withColorWrite(true, false)
            withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
        }
    }

    object GUI {
        @JvmField
        val Lines = newPipeline("gui/lines") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
        }

        @JvmField
        val Triangles = newPipeline("gui/triangles") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
        }
    }

    @JvmField
    val Lines = newPipeline("lines") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
        forWorldRender()
    }

    @JvmField
    val LineStrip = newPipeline("line_strip") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Triangles = newPipeline("triangles") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
        forWorldRender()
    }

    @JvmField
    val TriangleStrip = newPipeline("triangle_strip") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Quads = newPipeline("quads") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
        forWorldRender()
    }

    @JvmField
    val TexQuads = newPipeline("tex_quads") {
        withSnippet(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
        forWorldRender()
    }

    // Special

    @JvmField
    val Outline = newPipeline("outline") {
        withVertexShader(ClientShaders.SOBEL_VSH_ID)
        withFragmentShader(ClientShaders.OUTLINE_FSH_ID)
        withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLES)
        withSampler("texture0")
        withBlend(BlendFunction.ENTITY_OUTLINE_BLIT)
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    }

    @JvmField
    val ItemChams = newPipeline("item_chams") {
        withVertexShader(ClientShaders.PLANE_PROJECTION_VSH_ID)
        withFragmentShader(ClientShaders.GLOW_FSH_ID)
        withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLES)
        withSampler("texture0")
        withSampler("image")
        withUniform("ItemChamsData", UniformType.UNIFORM_BUFFER)
        withoutBlend()
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    }

    @JvmField
    val GuiBlur = newPipeline("blur") {
        withVertexShader(ClientShaders.SOBEL_VSH_ID)
        withFragmentShader(ClientShaders.BLUR_FSH_ID)
        withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLES)
        withSampler("texture0")
        withSampler("overlay")
        withUniform("BlurData", UniformType.UNIFORM_BUFFER)
        withoutBlend()
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    }

    @JvmField
    val Blend = newPipeline("blend") {
        withVertexShader(ClientShaders.PLAIN_POSITION_TEX_VSH_ID)
        withFragmentShader(ClientShaders.BLEND_FSH_ID)
        withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLES)
        withSampler("texture0")
        withUniform("BlendData", UniformType.UNIFORM_BUFFER)
        withoutBlend()
    }

    /**
     * Precompile
     */
    fun precompile() {
        JCEF
        GUI

        renderPipelines.fastIterator().forEach { (_, pipeline) ->
            gpuDevice.precompilePipeline(pipeline, ClientShaders)
        }
        logger.info("Loaded ${renderPipelines.size} Render Pipelines.")
    }

}
