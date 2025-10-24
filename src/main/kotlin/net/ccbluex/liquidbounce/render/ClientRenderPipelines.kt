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
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.VertexFormats
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.SynchronousResourceReloader
import net.minecraft.util.Identifier

// FIXME
object ClientRenderPipelines : SynchronousResourceReloader {

    private val renderPipelines = Object2ObjectRBTreeMap<Identifier, RenderPipeline>()

    /**
     * Blend mode for JCEF compatible blending.
     */
    private val JCEF_COMPATIBLE_BLEND = BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)

    private val COVERING_BLEND = BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA)

    private val BGRA_FSH_ID = LiquidBounce.identifier("fsh/bgra_pos_tex_color")
    private val BGRA_FSH_CODE = resourceToString("/resources/liquidbounce/shaders/bgra_position_tex_color.frag")

    private inline fun create(
        name: String,
        builderAction: RenderPipeline.Builder.() -> Unit,
    ): RenderPipeline {
        val id = LiquidBounce.identifier("pipeline/$name")
        return RenderPipeline.Builder()
            .withLocation(id)
            .apply(builderAction)
            .build().also { r ->
                renderPipelines.put(id, r)?.let { error("Duplicated render pipeline: $it") }
            }
    }

    // TODO: TEST THIS
    @Suppress("NOTHING_TO_INLINE")
    private inline fun RenderPipeline.Builder.bgraPosTexColorQuads() = apply {
        withVertexShader("core/position_tex_color")
        withFragmentShader(BGRA_FSH_ID)
        withSampler("Sampler0")
        withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
        withSnippet(RenderPipelines.MATRICES_SNIPPET)
        withSnippet(RenderPipelines.MATRICES_COLOR_SNIPPET)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun RenderPipeline.Builder.forWorldRender() = apply {
        withCull(false)
        withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        withBlend(COVERING_BLEND)
    }

    object JCEF {
        @JvmField
        val SMOOTH_TEXTURE = create("jcef/smooth_texture") {
            withSnippet(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            withBlend(BlendFunction.TRANSLUCENT)
            withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        }

        @JvmField
        val BLURRED_TEXTURE = create("jcef/blurred_texture") {
            withSnippet(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        }

        @JvmField
        val BGRA_TEXTURE = create("jcef/bgra_texture") {
            bgraPosTexColorQuads()
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        }

        @JvmField
        val BGRA_BLURRED_TEXTURE = create("jcef/bgra_blurred_texture") {
            bgraPosTexColorQuads()
            withBlend(JCEF_COMPATIBLE_BLEND)
            withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        }
    }

    @JvmField
    val Lines = create("lines") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
        forWorldRender()
    }

    @JvmField
    val LineStrip = create("line_strip") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Triangles = create("triangles") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
        forWorldRender()
    }

    @JvmField
    val TriangleStrip = create("triangle_strip") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
        forWorldRender()
    }

    @JvmField
    val Quads = create("quads") {
        withSnippet(RenderPipelines.POSITION_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
        forWorldRender()
    }

    @JvmField
    val TexQuads = create("tex_quads") {
        withSnippet(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
        withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
        forWorldRender()
    }

    /**
     * Precompile
     */
    override fun reload(manager: ResourceManager) {
        val device = RenderSystem.getDevice()

        renderPipelines.fastIterator().forEach { (_, pipeline) ->
            device.precompilePipeline(pipeline) { identifier, _ ->
                when (identifier) {
                    BGRA_FSH_ID -> BGRA_FSH_CODE
                    else -> error("Unknown identifier: $identifier")
                }
            }
        }
    }

}
