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

import com.mojang.blaze3d.pipeline.RenderTarget
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.engine.LazyRenderTargetHolder
import net.ccbluex.liquidbounce.render.getDynamicTransformsUniform
import net.ccbluex.liquidbounce.render.mesh.BatchCollector
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.renderer.PostChain

/**
 * World geometry drawn as silhouettes, which the chosen [PostStyle] then draws onto the scene.
 *
 * The style takes the color of the geometry and uses its alpha as strength, so geometry fades by lowering its alpha.
 * Colors set for [Rendering.Plain] reach it through [WorldRenderEnvironment.shapeColor], which makes them opaque.
 *
 * @param solid thin geometry like lines, which styles fill instead of only surrounding it
 */
class ShaderLayer(parent: ModeValueGroup<*>, solid: Boolean) : Rendering("Shader", parent) {

    val styles = modes(null, "Style", { DEFAULT_STYLE }) {
        postStyles(it, fill = if (solid) 1f else SHAPE_FILL)
    }

    private val chain = StyleChain(styles, PostChain.MAIN_TARGET_ID)
    private val collector = BatchCollector()

    private var frame: WorldRenderEvent? = null
    private var environment: WorldRenderEnvironment? = null

    override fun environment(event: WorldRenderEvent): WorldRenderEnvironment {
        environment?.takeIf { frame === event }?.let { return it }

        val target = silhouettes.get() ?: silhouettes.initAndGet()
        return WorldRenderEnvironment(target, event.poseStack, event.camera, collector, shaded = true).also {
            frame = event
            environment = it
            event.layers += this
        }
    }

    /**
     * What tells the pictures of two layers apart: the shader of their style and its settings.
     */
    private fun look() = styles.activeMode.let { it.pipeline to it.uniforms() }

    internal companion object {
        // Outline
        private const val DEFAULT_STYLE = 1

        private const val SHAPE_FILL = 0.15f

        private val silhouettes = LazyRenderTargetHolder("Shader layer silhouettes", useDepth = false)

        /**
         * Draws what [layers] collected this frame onto [scene]. Geometry of depth tested pipelines is cut by the
         * depth of [scene]. Layers that look the same share one silhouette target and one run of the post chain.
         */
        fun finish(layers: List<ShaderLayer>, scene: RenderTarget) {
            val drawn = layers.filter { layer ->
                layer.frame = null
                layer.environment = null
                !layer.collector.isEmpty
            }

            if (drawn.isEmpty()) {
                return
            }

            val transforms = getDynamicTransformsUniform()
            for (group in drawn.groupBy { it.look() }.values) {
                val target = silhouettes.initAndGet()
                for (layer in group) {
                    layer.collector.flush(target.colorTextureView!!, scene.depthTextureView, transforms)
                }

                group.first().chain.get()?.process(target, mc.gameRenderer.resourcePool)
                target.blitAndBlendToTexture(scene.colorTextureView!!, scene.depthTextureView!!)
            }
        }
    }

}
