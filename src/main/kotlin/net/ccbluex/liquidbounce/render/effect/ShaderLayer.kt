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
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
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
 * Draw into it with `event.renderEnvironment(layer) { ... }`. The style takes the color of the geometry and uses its
 * alpha as strength, so geometry fades by lowering its alpha.
 *
 * @param solid thin geometry like lines, which styles fill instead of only surrounding it
 * @param name a module with several layers names them apart
 */
class ShaderLayer(solid: Boolean = false, name: String = "Shader") : ValueGroup(name) {

    val styles = modes(null, "Style", { DEFAULT_STYLE }) {
        postStyles(it, fill = if (solid) 1f else SHAPE_FILL).toTypedArray()
    }

    private val chain = StyleChain(styles, PostChain.MAIN_TARGET_ID)
    private val collector = BatchCollector()
    private val passes = ArrayList<(RenderTarget) -> Unit>()

    private var frame: WorldRenderEvent? = null
    private var environment: WorldRenderEnvironment? = null

    fun environment(event: WorldRenderEvent): WorldRenderEnvironment {
        environment?.takeIf { frame === event }?.let { return it }

        val target = silhouettes.get() ?: silhouettes.initAndGet()
        return WorldRenderEnvironment(target, event.poseStack, event.camera, collector).also {
            frame = event
            environment = it
            event.layers += this
        }
    }

    /**
     * For geometry that comes with a render pass of its own: [pass] draws it into the silhouette target it is given
     * once the frame is finished. Translucent blending is required, see `field_rows.frag`.
     */
    internal fun draw(event: WorldRenderEvent, pass: (RenderTarget) -> Unit) {
        environment(event)
        passes += pass
    }

    /**
     * Draws what the frame collected onto [scene]. Geometry of depth tested pipelines is cut by the depth of [scene].
     */
    internal fun finish(scene: RenderTarget) {
        frame = null
        environment = null
        if (collector.isEmpty && passes.isEmpty()) {
            return
        }

        // Layers are finished one after another, so they share the target
        val target = silhouettes.initAndGet()
        collector.flush(target.colorTextureView!!, scene.depthTextureView, getDynamicTransformsUniform())
        passes.forEach { it(target) }
        passes.clear()

        chain.get()?.process(target, mc.gameRenderer.resourcePool)
        target.blitAndBlendToTexture(scene.colorTextureView!!, scene.depthTextureView!!)
    }

    private companion object {
        // Outline
        const val DEFAULT_STYLE = 1

        const val SHAPE_FILL = 0.15f

        val silhouettes = LazyRenderTargetHolder("Shader layer silhouettes", useDepth = false)
    }

}
