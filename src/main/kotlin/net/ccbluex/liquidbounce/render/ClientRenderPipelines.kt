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

@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")
package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object ClientRenderPipelines {

    private val renderPipelines = Object2ObjectOpenHashMap<Identifier, RenderPipeline>()

    /**
     * Blend mode for JCEF compatible blending.
     */
    private val JCEF_COMPATIBLE_BLEND = BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA

    internal inline fun newPipeline(
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

    private inline fun RenderPipeline.Builder.bgraPosTexColorQuads() {
        withSnippet(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        withVertexShader("core/position_tex_color")
        withFragmentShader(ClientShaders.Fragment.BgraPosTex)
        withSampler("Sampler0")
        withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
    }

    inline fun RenderPipeline.Builder.withUniformBuffer(define: ClientUniformDefine) =
        withUniform(define.uboName, UniformType.UNIFORM_BUFFER)

    private inline fun RenderPipeline.Builder.forWorldRender(noDepthTest: Boolean = true) {
        withCull(false)
        withBlend(BlendFunction.TRANSLUCENT)
        if (noDepthTest) withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    }

    inline fun RenderPipeline.Builder.screenQuadSnippet() = apply {
        withVertexShader("core/screenquad")
        withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
    }

    private fun RenderPipeline.Builder.posColorSnippet(mode: VertexFormat.Mode) {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, mode)
    }

    private inline fun RenderPipeline.Builder.relativePosSnippet(mode: VertexFormat.Mode) {
        withVertexShader(ClientShaders.Vertex.PosRelativeToCamera)
        withFragmentShader(ClientShaders.Fragment.PosRelativeToCamera)
        withVertexFormat(DefaultVertexFormat.POSITION, mode)
    }

    private inline fun RenderPipeline.Builder.relativePosColorSnippet(mode: VertexFormat.Mode) {
        withVertexShader(ClientShaders.Vertex.PosColorRelativeToCamera)
        withFragmentShader("core/position_color")
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, mode)
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
            screenQuadSnippet()
            withFragmentShader("core/blit_screen")
            withSampler("InSampler")
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthWrite(false)
            withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            withColorWrite(true, false)
        }
    }

    object GUI {
        private fun RenderPipeline.Builder.guiPosColorSnippet(mode: VertexFormat.Mode) {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, mode)
        }

        private val Lines = newPipeline("gui/lines") {
            guiPosColorSnippet(VertexFormat.Mode.DEBUG_LINES)
        }

        private val Triangles = newPipeline("gui/triangles") {
            guiPosColorSnippet(VertexFormat.Mode.TRIANGLES)
        }

        private val LinesNoCull = newPipeline("gui/lines_no_cull") {
            guiPosColorSnippet(VertexFormat.Mode.DEBUG_LINES)
            withCull(false)
        }

        private val TrianglesNoCull = newPipeline("gui/triangles_no_cull") {
            guiPosColorSnippet(VertexFormat.Mode.TRIANGLES)
            withCull(false)
        }

        @JvmField
        val TexQuadNoCull = newPipeline("gui/tex_quad_no_cull") {
            withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
            withCull(false)
        }

        @JvmStatic
        fun lines(cull: Boolean) = if (cull) Lines else LinesNoCull

        @JvmStatic
        fun triangles(cull: Boolean) = if (cull) Triangles else TrianglesNoCull
    }

    /**
     * @see RenderPipelines.LINES_TRANSLUCENT
     */
    @JvmField
    val LinesWithWidth = newPipeline("lines_with_width") {
        withSnippet(RenderPipelines.LINES_SNIPPET)
        withDepthWrite(false)
        forWorldRender()
    }

    @JvmField
    val Lines = newPipeline("lines") {
        posColorSnippet(VertexFormat.Mode.DEBUG_LINES)
        forWorldRender()
    }

    private val LinesRelativeToCamera = newPipeline("lines_relative_to_camera") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosColorSnippet(VertexFormat.Mode.DEBUG_LINES)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        forWorldRender()
    }

    private val LinesRelativeToCameraNoColor = newPipeline("lines_relative_to_camera_no_color") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosColorSnippet(VertexFormat.Mode.DEBUG_LINES)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        forWorldRender()
    }

    @JvmStatic
    fun relativeLines(useColor: Boolean) = if (useColor) LinesRelativeToCamera else LinesRelativeToCameraNoColor

    @JvmField
    val LineStrip = newPipeline("line_strip") {
        posColorSnippet(VertexFormat.Mode.DEBUG_LINE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Triangles = newPipeline("triangles") {
        posColorSnippet(VertexFormat.Mode.TRIANGLES)
        forWorldRender()
    }

    private val TriangleStrip = newPipeline("triangle_strip") {
        posColorSnippet(VertexFormat.Mode.TRIANGLE_STRIP)
        forWorldRender(noDepthTest = false)
    }

    private val TriangleStripNoDepthTest = newPipeline("triangle_strip_no_depth_test") {
        posColorSnippet(VertexFormat.Mode.TRIANGLE_STRIP)
        forWorldRender(noDepthTest = true)
    }

    @JvmStatic
    fun triangleStrip(noDepthTest: Boolean) = if (noDepthTest) TriangleStripNoDepthTest else TriangleStrip

    @JvmField
    val Quads = newPipeline("quads") {
        posColorSnippet(VertexFormat.Mode.QUADS)
        forWorldRender()
    }

    private val QuadsRelativeToCamera = newPipeline("quads_relative_to_camera") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosColorSnippet(VertexFormat.Mode.QUADS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        forWorldRender()
    }

    private val QuadsRelativeToCameraNoColor = newPipeline("quads_relative_to_camera_no_color") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosSnippet(VertexFormat.Mode.QUADS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        forWorldRender()
    }

    @JvmStatic
    fun relativeQuads(useColor: Boolean) = if (useColor) QuadsRelativeToCamera else QuadsRelativeToCameraNoColor

    /**
     * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP
     * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleBlockESP
     */
    private val OutlineQuads = newPipeline("outline_quads") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withSnippet(RenderPipelines.GLOBALS_SNIPPET)
        withVertexShader(ClientShaders.Vertex.PosColorRelativeToCamera)
        withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        withBlend(BlendFunction.TRANSLUCENT)
    }

    private val OutlineQuadsNoColor = newPipeline("outline_quads_no_color") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withSnippet(RenderPipelines.GLOBALS_SNIPPET)
        withVertexShader(ClientShaders.Vertex.PosRelativeToCamera)
        withFragmentShader(ClientShaders.Fragment.PosRelativeToCamera)
        withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        withBlend(BlendFunction.TRANSLUCENT)
    }

    @JvmStatic
    fun outlineQuads(useColor: Boolean) = if (useColor) OutlineQuads else OutlineQuadsNoColor

    @JvmField
    val TexQuads = newPipeline("tex_quads") {
        withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
        withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        forWorldRender()
    }

    private fun RenderPipeline.Builder.roundedRectSnippet() {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexShader(ClientShaders.Vertex.Circle)
        withFragmentShader(ClientShaders.Fragment.RoundedRect)
        withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        withUniformBuffer(ClientUniformDefine.ROUNDED_RECT)
    }

    private val RoundedRect = newPipeline("rounded_rect") {
        roundedRectSnippet()
        forWorldRender(noDepthTest = false)
    }

    private val RoundedRectNoDepthTest = newPipeline("rounded_rect_no_depth_test") {
        roundedRectSnippet()
        forWorldRender(noDepthTest = true)
    }

    fun roundedRect(noDepthTest: Boolean) = if (noDepthTest) RoundedRectNoDepthTest else RoundedRect

    // Special

    /**
     * @see RenderPipelines.ENTITY_OUTLINE_BLIT
     * @see RenderPipelines.OUTLINE_SNIPPET
     */
    @JvmField
    val Outline = newPipeline("outline") {
        screenQuadSnippet()
        withFragmentShader(ClientShaders.Fragment.EntityOutline)
        withSampler("InSampler")
        withBlend(BlendFunction.ENTITY_OUTLINE_BLIT)
        withDepthWrite(false)
        withColorWrite(true, false)
    }

    @JvmField
    val ItemChams = newPipeline("item_chams") {
        screenQuadSnippet()
        withFragmentShader(ClientShaders.Fragment.Glow)
        withSampler("texture0")
        withSampler("image")
        withUniformBuffer(ClientUniformDefine.HAND_ITEM_LIGHTMAP)
        withoutBlend()
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        withDepthWrite(false)
    }

    @JvmField
    val GuiBlur = newPipeline("blur") {
        screenQuadSnippet()
        withFragmentShader(ClientShaders.Fragment.GuiBlur)
        withSampler("texture0")
        withSampler("overlay")
        withUniformBuffer(ClientUniformDefine.GUI_BLUR)
        withCull(false)
        withoutBlend()
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        withDepthWrite(false)
    }

    @JvmField
    val Blend = newPipeline("blend") {
        withVertexShader(ClientShaders.Vertex.PlainPosTex)
        withFragmentShader(ClientShaders.Fragment.Blend)
        withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
        withSampler("texture0")
        withUniformBuffer(ClientUniformDefine.BLEND)
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
