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
import net.minecraft.client.renderer.RenderPipelines
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.resources.Identifier

object ClientRenderPipelines {

    private val renderPipelines = Object2ObjectOpenHashMap<Identifier, RenderPipeline>()

    /**
     * Blend mode for JCEF compatible blending.
     */
    private val JCEF_COMPATIBLE_BLEND = BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA

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
    private inline fun RenderPipeline.Builder.bgraPosTexColorQuads() {
        withVertexShader("core/position_tex_color")
        withFragmentShader(ClientShaders.BGRA_FSH_ID)
        withSampler("Sampler0")
        withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        withSnippet(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun RenderPipeline.Builder.screenQuad() = apply {
        withVertexShader("core/screenquad")
        withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun RenderPipeline.Builder.forWorldRender() {
        withCull(false)
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        withBlend(COVERING_BLEND)
    }

    object JCEF {
        @JvmField
        val SMOOTH_TEXTURE = newPipeline("jcef/smooth_texture") {
            withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
            withBlend(BlendFunction.TRANSLUCENT)
            withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        }

        @JvmField
        val BLURRED_TEXTURE = newPipeline("jcef/blurred_texture") {
            withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
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

        /**
         * @see RenderPipelines.ENTITY_OUTLINE_BLIT
         */
        @JvmField
        val Blit = newPipeline("jcef_blit") {
            screenQuad()
            withFragmentShader("core/blit_screen")
            withSampler("InSampler")
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthWrite(false)
            withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            withColorWrite(true, false)
        }
    }

    object GUI {
        private val Lines = newPipeline("gui/lines") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
        }

        private val Triangles = newPipeline("gui/triangles") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        }

        private val LinesNoCull = newPipeline("gui/lines_no_cull") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withCull(false)
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
        }

        private val TrianglesNoCull = newPipeline("gui/triangles_no_cull") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withCull(false)
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        }

        @JvmStatic
        fun lines(cull: Boolean) = if (cull) Lines else LinesNoCull

        @JvmStatic
        fun triangles(cull: Boolean) = if (cull) Triangles else TrianglesNoCull
    }

    @JvmField
    val Lines = newPipeline("lines") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
        forWorldRender()
    }

    @JvmField
    val LineStrip = newPipeline("line_strip") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Triangles = newPipeline("triangles") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        forWorldRender()
    }

    @JvmField
    val TriangleStrip = newPipeline("triangle_strip") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Quads = newPipeline("quads") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        forWorldRender()
    }

    /**
     * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP
     * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleBlockESP
     */
    @JvmField
    val OutlineQuads = newPipeline("outline_quads") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        withBlend(COVERING_BLEND)
    }

    @JvmField
    val TexQuads = newPipeline("tex_quads") {
        withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        forWorldRender()
    }

    // Special

    /**
     * @see RenderPipelines.ENTITY_OUTLINE_BLIT
     * @see RenderPipelines.OUTLINE_SNIPPET
     */
    @JvmField
    val Outline = newPipeline("outline") {
        screenQuad()
        withFragmentShader(ClientShaders.OUTLINE_FSH_ID)
        withSampler("InSampler")
        withBlend(BlendFunction.ENTITY_OUTLINE_BLIT)
        withDepthWrite(false)
        withColorWrite(true, false)
    }

    @JvmField
    val ItemChams = newPipeline("item_chams") {
        screenQuad()
        withFragmentShader(ClientShaders.GLOW_FSH_ID)
        withSampler("texture0")
        withSampler("image")
        withUniform("ItemChamsData", UniformType.UNIFORM_BUFFER)
        withoutBlend()
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        withDepthWrite(false)
    }

    @JvmField
    val GuiBlur = newPipeline("blur") {
        screenQuad()
        withFragmentShader(ClientShaders.BLUR_FSH_ID)
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
        withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
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
