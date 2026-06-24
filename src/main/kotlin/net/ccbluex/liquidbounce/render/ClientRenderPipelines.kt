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

import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.optional
import net.minecraft.client.renderer.BindGroupLayouts
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
        withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        withVertexShader("core/position_tex_color")
        withFragmentShader(ClientShaders.Fragment.BgraPosTex)
        withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        withPrimitiveTopology(PrimitiveTopology.QUADS)
    }

    inline fun RenderPipeline.Builder.withBindGroupLayout(block: BindGroupLayout.Builder.() -> Unit) =
        this.withBindGroupLayout(BindGroupLayout.builder().apply(block).build())

    inline fun RenderPipeline.Builder.withUniformBuffer(define: ClientUniformDefine) =
        withBindGroupLayout(define.bindGroupLayout)

    private inline fun RenderPipeline.Builder.forWorldRender(noDepthTest: Boolean = true) {
        withCull(false)
        withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        if (noDepthTest) withDepthStencilState(optional())
    }

    inline fun RenderPipeline.Builder.screenQuadSnippet() = apply {
        withVertexShader("core/screenquad")
        withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    }

    private fun RenderPipeline.Builder.posColorSnippet(mode: PrimitiveTopology) {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        withPrimitiveTopology(mode)
    }

    private inline fun RenderPipeline.Builder.relativePosSnippet(mode: PrimitiveTopology) {
        withVertexShader(ClientShaders.Vertex.PosRelativeToCamera)
        withFragmentShader(ClientShaders.Fragment.PosRelativeToCamera)
        withVertexBinding(0, DefaultVertexFormat.POSITION)
        withPrimitiveTopology(mode)
    }

    private inline fun RenderPipeline.Builder.relativePosColorSnippet(mode: PrimitiveTopology) {
        withVertexShader(ClientShaders.Vertex.PosColorRelativeToCamera)
        withFragmentShader("core/position_color")
        withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        withPrimitiveTopology(mode)
    }

    object JCEF {
        @JvmField
        val SMOOTH_TEXTURE = newPipeline("jcef/smooth_texture") {
            withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withDepthStencilState(optional())
        }

        @JvmField
        val BLURRED_TEXTURE = newPipeline("jcef/blurred_texture") {
            withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
            withColorTargetState(ColorTargetState(JCEF_COMPATIBLE_BLEND))
        }

        @JvmField
        val BGRA_TEXTURE = newPipeline("jcef/bgra_texture") {
            bgraPosTexColorQuads()
            withColorTargetState(ColorTargetState(JCEF_COMPATIBLE_BLEND))
        }

        @JvmField
        val BGRA_BLURRED_TEXTURE = newPipeline("jcef/bgra_blurred_texture") {
            bgraPosTexColorQuads()
            withColorTargetState(ColorTargetState(JCEF_COMPATIBLE_BLEND))
        }

        /**
         * @see RenderPipelines.ENTITY_OUTLINE_BLIT
         */
        @JvmField
        val Blit = newPipeline("jcef_blit") {
            screenQuadSnippet()
            withFragmentShader("core/blit_screen")
            withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
            withColorTargetState(
                ColorTargetState(
                    optional(JCEF_COMPATIBLE_BLEND),
                    GpuFormat.RGBA8_UNORM,
                    ColorTargetState.WRITE_COLOR,
                )
            )
            withDepthStencilState(optional())
        }
    }

    object GUI {
        private fun RenderPipeline.Builder.guiPosColorSnippet(mode: PrimitiveTopology) {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(mode)
        }

        private val CircleLut = newPipeline("gui/circle_lut") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexShader(ClientShaders.Vertex.GuiCircleLut)
            withFragmentShader(ClientShaders.Fragment.GuiCircleLut)
            withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            withVertexBinding(0, ClientVertexFormats.GUI_CIRCLE_LUT)
            withPrimitiveTopology(PrimitiveTopology.QUADS)
        }

        private val RoundedRect = newPipeline("gui/rounded_rect") {
            withSnippet(RenderPipelines.GUI_SNIPPET)
            withVertexShader(ClientShaders.Vertex.GuiRoundedRect)
            withFragmentShader(ClientShaders.Fragment.GuiRoundedRect)
            withVertexBinding(0, ClientVertexFormats.GUI_ROUNDED_RECT)
            withPrimitiveTopology(PrimitiveTopology.QUADS)
        }

        private val Lines = newPipeline("gui/lines") {
            guiPosColorSnippet(PrimitiveTopology.DEBUG_LINES)
        }

        private val Triangles = newPipeline("gui/triangles") {
            guiPosColorSnippet(PrimitiveTopology.TRIANGLES)
        }

        private val LinesNoCull = newPipeline("gui/lines_no_cull") {
            guiPosColorSnippet(PrimitiveTopology.DEBUG_LINES)
            withCull(false)
        }

        private val TrianglesNoCull = newPipeline("gui/triangles_no_cull") {
            guiPosColorSnippet(PrimitiveTopology.TRIANGLES)
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

        @JvmStatic
        fun circleLut() = CircleLut

        @JvmStatic
        fun roundedRect() = RoundedRect
    }

    /**
     * @see RenderPipelines.LINES_TRANSLUCENT
     */
    @JvmField
    val LinesWithWidth = newPipeline("lines_with_width") {
        withSnippet(RenderPipelines.LINES_SNIPPET)
        withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        forWorldRender()
    }

    @JvmField
    val Lines = newPipeline("lines") {
        posColorSnippet(PrimitiveTopology.DEBUG_LINES)
        forWorldRender()
    }

    private val LinesRelativeToCamera = newPipeline("lines_relative_to_camera") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosColorSnippet(PrimitiveTopology.DEBUG_LINES)
        withUniformBuffer(ClientUniformDefine.MESH_BASE_BLOCK_POS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        forWorldRender()
    }

    private val LinesRelativeToCameraNoColor = newPipeline("lines_relative_to_camera_no_color") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosColorSnippet(PrimitiveTopology.DEBUG_LINES)
        withUniformBuffer(ClientUniformDefine.MESH_BASE_BLOCK_POS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        forWorldRender()
    }

    @JvmStatic
    fun relativeLines(useColor: Boolean) = if (useColor) LinesRelativeToCamera else LinesRelativeToCameraNoColor

    @JvmField
    val LineStrip = newPipeline("line_strip") {
        posColorSnippet(PrimitiveTopology.DEBUG_LINE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Triangles = newPipeline("triangles") {
        posColorSnippet(PrimitiveTopology.TRIANGLES)
        forWorldRender()
    }

    private val TriangleStrip = newPipeline("triangle_strip") {
        posColorSnippet(PrimitiveTopology.TRIANGLE_STRIP)
        forWorldRender(noDepthTest = false)
    }

    private val TriangleStripNoDepthTest = newPipeline("triangle_strip_no_depth_test") {
        posColorSnippet(PrimitiveTopology.TRIANGLE_STRIP)
        forWorldRender(noDepthTest = true)
    }

    @JvmStatic
    fun triangleStrip(noDepthTest: Boolean) = if (noDepthTest) TriangleStripNoDepthTest else TriangleStrip

    @JvmField
    val Quads = newPipeline("quads") {
        posColorSnippet(PrimitiveTopology.QUADS)
        forWorldRender()
    }

    private val QuadsRelativeToCamera = newPipeline("quads_relative_to_camera") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosColorSnippet(PrimitiveTopology.QUADS)
        withUniformBuffer(ClientUniformDefine.MESH_BASE_BLOCK_POS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        forWorldRender()
    }

    private val QuadsRelativeToCameraNoColor = newPipeline("quads_relative_to_camera_no_color") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        relativePosSnippet(PrimitiveTopology.QUADS)
        withUniformBuffer(ClientUniformDefine.MESH_BASE_BLOCK_POS)
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
        withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        withPrimitiveTopology(PrimitiveTopology.QUADS)
        withUniformBuffer(ClientUniformDefine.MESH_BASE_BLOCK_POS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
    }

    private val OutlineQuadsNoColor = newPipeline("outline_quads_no_color") {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withSnippet(RenderPipelines.GLOBALS_SNIPPET)
        withVertexShader(ClientShaders.Vertex.PosRelativeToCamera)
        withFragmentShader(ClientShaders.Fragment.PosRelativeToCamera)
        withVertexBinding(0, DefaultVertexFormat.POSITION)
        withPrimitiveTopology(PrimitiveTopology.QUADS)
        withUniformBuffer(ClientUniformDefine.MESH_BASE_BLOCK_POS)
        withUniformBuffer(ClientUniformDefine.DISTANCE_FADE)
        withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
    }

    @JvmStatic
    fun outlineQuads(useColor: Boolean) = if (useColor) OutlineQuads else OutlineQuadsNoColor

    @JvmField
    val TexQuads = newPipeline("tex_quads") {
        withSnippet(RenderPipelines.GUI_TEXTURED_SNIPPET)
        withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        withPrimitiveTopology(PrimitiveTopology.QUADS)
        forWorldRender()
    }

    private fun RenderPipeline.Builder.roundedRectSnippet() {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexShader(ClientShaders.Vertex.Circle)
        withFragmentShader(ClientShaders.Fragment.RoundedRect)
        withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        withPrimitiveTopology(PrimitiveTopology.QUADS)
        withUniformBuffer(ClientUniformDefine.ROUNDED_RECT)
    }

    private fun RenderPipeline.Builder.gradientCircleSnippet() {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexShader(ClientShaders.Vertex.GradientCircle)
        withFragmentShader(ClientShaders.Fragment.GradientCircle)
        withVertexBinding(0, ClientVertexFormats.GRADIENT_CIRCLE)
        withPrimitiveTopology(PrimitiveTopology.QUADS)
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

    private val GradientCircle = newPipeline("gradient_circle") {
        gradientCircleSnippet()
        forWorldRender(noDepthTest = false)
    }

    private val GradientCircleNoDepthTest = newPipeline("gradient_circle_no_depth_test") {
        gradientCircleSnippet()
        forWorldRender(noDepthTest = true)
    }

    fun gradientCircle(noDepthTest: Boolean) =
        if (noDepthTest) GradientCircleNoDepthTest else GradientCircle

    private fun RenderPipeline.Builder.heartSdfSnippet() {
        withSnippet(RenderPipelines.DEBUG_FILLED_SNIPPET)
        withVertexShader(ClientShaders.Vertex.Circle)
        withFragmentShader(ClientShaders.Fragment.HeartSDF)
        withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        withPrimitiveTopology(PrimitiveTopology.QUADS)
    }

    private val Heart = newPipeline("heart") {
        heartSdfSnippet()
        forWorldRender(noDepthTest = false)
    }

    private val HeartNoDepthTest = newPipeline("heart_no_depth_test") {
        heartSdfSnippet()
        forWorldRender(noDepthTest = true)
    }

    fun heart(noDepthTest: Boolean) = if (noDepthTest) HeartNoDepthTest else Heart

    // Special

    /**
     * @see RenderPipelines.ENTITY_OUTLINE_BLIT
     * @see RenderPipelines.OUTLINE_SNIPPET
     */
    @JvmField
    val Outline = newPipeline("outline") {
        screenQuadSnippet()
        withFragmentShader(ClientShaders.Fragment.EntityOutline)
        withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
        withColorTargetState(
            ColorTargetState(
                optional(BlendFunction.ENTITY_OUTLINE_BLIT),
                GpuFormat.RGBA8_UNORM,
                ColorTargetState.WRITE_COLOR,
            )
        )
        withDepthStencilState(optional())
    }

    @JvmField
    val ItemChams = newPipeline("item_chams") {
        screenQuadSnippet()
        withFragmentShader(ClientShaders.Fragment.Glow)
        withBindGroupLayout {
            withSampler("texture0")
            withSampler("image")
        }
        withUniformBuffer(ClientUniformDefine.HAND_ITEM_LIGHTMAP)
        withColorTargetState(ColorTargetState.DEFAULT)
        withDepthStencilState(optional())
    }

    @JvmField
    val GuiBlur = newPipeline("blur") {
        screenQuadSnippet()
        withFragmentShader(ClientShaders.Fragment.GuiBlur)
        withBindGroupLayout {
            withSampler("texture0")
            withSampler("overlay")
        }
        withUniformBuffer(ClientUniformDefine.GUI_BLUR)
        withCull(false)
        withColorTargetState(ColorTargetState.DEFAULT)
        withDepthStencilState(optional())
    }

    @JvmField
    val Blend = newPipeline("blend") {
        withVertexShader(ClientShaders.Vertex.PlainPosTex)
        withFragmentShader(ClientShaders.Fragment.Blend)
        withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        withBindGroupLayout {
            withSampler("texture0")
        }
        withUniformBuffer(ClientUniformDefine.BLEND)
        withColorTargetState(ColorTargetState.DEFAULT)
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
