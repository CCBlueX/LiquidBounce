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

package net.ccbluex.liquidbounce.render.effect

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.UniformType
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.render.ClientShaders
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.renderer.PostChain
import net.minecraft.client.renderer.PostChainConfig
import net.minecraft.client.renderer.PostPass
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.UniformValue
import net.minecraft.resources.Identifier
import org.joml.Vector4f
import java.util.Optional
import kotlin.math.tan

/**
 * Post chain drawing the active [PostStyle] of [styles] over the silhouettes in the target [target].
 *
 * Two passes turn the silhouettes into the distance to and color of the nearest silhouette pixel, the style then draws
 * from that back into the target, which is blended onto the scene.
 *
 * The pipelines are built here instead of through [PostChain.load], which would create and compile new ones on every
 * change of a style value.
 */
class StyleChain(private val styles: ModeValueGroup<out ShaderStyle>, private val target: Identifier) : AutoCloseable {

    private var chain: PostChain? = null
    private var chainStyle: PostStyle? = null
    private val views = ArrayList<GpuBufferSlice>(1)
    private var dirty = true

    init {
        for (style in styles.modes) {
            for (value in style.containedValues) {
                value.onChanged { dirty = true }
            }
        }
    }

    /**
     * The chain of the active style, null when that is no [PostStyle] or does not compile.
     */
    fun get(): PostChain? {
        val style = styles.activeMode as? PostStyle
        if (style == null) {
            close()
            return null
        }

        if (dirty || style !== chainStyle) {
            close()
            chain = if (compiles(style)) build(style) else null
            chainStyle = style
            dirty = false
        }
        views.forEach(::writeView)
        return chain
    }

    override fun close() {
        chain?.close()
        chain = null
        chainStyle = null
        views.clear()
    }

    private fun build(style: PostStyle) = postChain(
        stylePasses(style, target, target, views),
        mapOf(rows to scratch, field to scratch),
        setOf(target),
    )

    companion object {

        private const val UNIFORMS = "EffectData"

        private val id = LiquidBounce.identifier("effect")
        private val rows = LiquidBounce.identifier("effect/rows")
        private val field = LiquidBounce.identifier("effect/field")

        /**
         * Screen sized target of a chain, cleared before every frame.
         */
        internal val scratch = PostChainConfig.InternalTarget(Optional.empty(), Optional.empty(), false, 0)

        internal val scratchTargets = arrayOf(rows, field)

        private val pipelines = HashMap<Identifier, RenderPipeline>()

        val FieldRows = pipeline(ClientShaders.Fragment.EffectRows)
        val FieldColumns = pipeline(ClientShaders.Fragment.EffectColumns)

        fun compiles(style: PostStyle) = arrayOf(FieldRows, FieldColumns, style.pipeline).all {
            RenderSystem.getCompiledPipelineNullable(it) != null
        }

        internal fun postChain(
            passes: List<PostPass>,
            internalTargets: Map<Identifier, PostChainConfig.InternalTarget>,
            externalTargets: Set<Identifier>,
        ) = PostChain(
            id,
            passes,
            internalTargets,
            externalTargets,
            mc.shaderManager.postChainProjection,
            mc.shaderManager.postChainProjectionMatrixBuffer,
        )

        /**
         * The passes drawing [style] from the silhouettes in [input] into [output], using the scratch targets
         * [scratchTargets]. Buffers that [writeView] has to fill every frame go to [views].
         */
        internal fun stylePasses(
            style: PostStyle,
            input: Identifier,
            output: Identifier,
            views: MutableList<GpuBufferSlice>,
        ): List<PostPass> {
            val uniforms = mapOf(UNIFORMS to style.uniforms())
            val stylePass = pass(style.pipeline, field, output, uniforms)
            if (style.usesView) {
                // The pass closes the buffer with its own
                views += ClientUniformDefine.EFFECT_VIEW.createSingleBuffer().also {
                    stylePass.customUniforms[ClientUniformDefine.EFFECT_VIEW.uboName] = it.buffer
                }
            }

            return listOf(
                pass(FieldRows, input, rows, uniforms),
                pass(FieldColumns, rows, field, uniforms),
                stylePass,
            )
        }

        internal fun pass(
            pipeline: RenderPipeline,
            input: Identifier,
            output: Identifier,
            uniforms: Map<String, List<UniformValue>> = emptyMap(),
        ) = PostPass(pipeline, output, uniforms, listOf(PostPass.TargetInput("In", input, false, false)))

        /**
         * Camera axes and the tangents of half the field of view, see `EffectView` in `effect.glsl`.
         */
        internal fun writeView(view: GpuBufferSlice) {
            val camera = mc.gameRenderer.mainCamera()
            val tangent = tan(camera.fov.toRadians() / 2f)
            val left = camera.leftVector()
            view.writeStd140 {
                putVec4(-left.x(), -left.y(), -left.z(), 0f)
                putVec4(Vector4f(camera.upVector(), 0f))
                putVec4(Vector4f(camera.forwardVector(), 0f))
                putVec4(tangent * mc.window.width / mc.window.height, tangent, 0f, 0f)
            }
        }

        /**
         * A pass reading `InSampler` and the `EffectData` block, like [PostChain.load] builds them, with
         * [usesView] the `EffectView` block as well.
         */
        fun pipeline(shader: Identifier, usesView: Boolean = false): RenderPipeline = pipelines.getOrPut(shader) {
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(shader)
                .withVertexShader(ClientShaders.Vertex.ScreenQuad)
                .withFragmentShader(shader)
                .withBindGroupLayout(
                    BindGroupLayout.builder()
                        .withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                        .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                        .withUniform(UNIFORMS, UniformType.UNIFORM_BUFFER)
                        .apply { if (usesView) ClientUniformDefine.EFFECT_VIEW.appendTo(this) }
                        .build()
                )
                .withColorTargetState(ColorTargetState.DEFAULT)
                .build()
        }

    }

}
