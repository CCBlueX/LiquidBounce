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

package net.ccbluex.liquidbounce.features.module.modules.render.esp.glow

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.UniformType
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode
import net.ccbluex.liquidbounce.render.ClientShaders
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.renderer.LevelTargetBundle
import net.minecraft.client.renderer.PostChain
import net.minecraft.client.renderer.PostChainConfig
import net.minecraft.client.renderer.PostPass
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.UniformValue
import net.minecraft.resources.Identifier
import java.util.Optional

/**
 * Post chain of the selected [ShaderGlow] style, in place of the game's entity outline post chain.
 *
 * Two passes turn the silhouettes in the outline target into the distance to and color of the nearest silhouette
 * pixel, the style then draws from that back into the outline target, which the game blends onto the scene.
 *
 * The pipelines are built here instead of through [PostChain.load], which would create and compile new ones on every
 * change of a style value.
 */
object GlowPostEffect {

    private const val UNIFORMS = "EspGlowData"

    private val id = LiquidBounce.identifier("esp_glow")
    private val rows = LiquidBounce.identifier("esp_glow/rows")
    private val field = LiquidBounce.identifier("esp_glow/field")
    private val outline = LevelTargetBundle.ENTITY_OUTLINE_TARGET_ID
    private val screenTarget = PostChainConfig.InternalTarget(Optional.empty(), Optional.empty(), false, 0)

    val FieldRows = pipeline(ClientShaders.Fragment.EspGlowRows)
    val FieldColumns = pipeline(ClientShaders.Fragment.EspGlowColumns)

    private var chain: PostChain? = null
    private var chainStyle: ShaderGlow? = null
    private var dirty = true

    fun invalidate() {
        dirty = true
    }

    @JvmStatic
    fun replace(original: PostChain?): PostChain? {
        val style = EspGlowMode.styles.activeMode
        if (!EspGlowMode.running || style !is ShaderGlow) {
            return original
        }

        if (dirty || style !== chainStyle) {
            chain?.close()
            chain = if (compiles(style)) build(style) else null
            chainStyle = style
            dirty = false
        }
        return chain ?: original
    }

    fun compiles(style: ShaderGlow) = arrayOf(FieldRows, FieldColumns, style.pipeline).all {
        RenderSystem.getCompiledPipelineNullable(it) != null
    }

    private fun build(style: ShaderGlow): PostChain {
        val uniforms = mapOf(UNIFORMS to style.uniforms())
        return PostChain(
            id,
            listOf(
                pass(FieldRows, outline, rows, uniforms),
                pass(FieldColumns, rows, field, uniforms),
                pass(style.pipeline, field, outline, uniforms),
            ),
            mapOf(rows to screenTarget, field to screenTarget),
            setOf(outline),
            mc.shaderManager.postChainProjection,
            mc.shaderManager.postChainProjectionMatrixBuffer,
        )
    }

    private fun pass(
        pipeline: RenderPipeline,
        input: Identifier,
        output: Identifier,
        uniforms: Map<String, List<UniformValue>>,
    ) = PostPass(pipeline, output, uniforms, listOf(PostPass.TargetInput("In", input, false, false)))

    /**
     * A pass reading `InSampler` and the `EspGlowData` block, like [PostChain.load] builds them.
     */
    fun pipeline(shader: Identifier): RenderPipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
        .withLocation(shader)
        .withVertexShader(ClientShaders.Vertex.ScreenQuad)
        .withFragmentShader(shader)
        .withBindGroupLayout(
            BindGroupLayout.builder()
                .withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                .withUniform(UNIFORMS, UniformType.UNIFORM_BUFFER)
                .build()
        )
        .withColorTargetState(ColorTargetState.DEFAULT)
        .build()

}
