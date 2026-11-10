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

package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTextureView
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.buffers.CachedUniform
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.setPipeline
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.putVec4
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.util.Util
import org.joml.Vector4f
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the [ModuleItemChams] effect layer on top of the first person hands and items.
 *
 * The vanilla hand pass keeps rendering into the main render target untouched. The very same
 * [FeatureRenderDispatcher.PreparedFrame] is drawn a second time into an offscreen target, which
 * provides both the held item colors and its coverage mask (private depth buffer), so the effects
 * can be composed without touching the vanilla blending, alpha or depth semantics.
 *
 * @see net.minecraft.client.renderer.GameRenderer
 */
object ItemChamsEffectRenderer : MinecraftShortcuts {

    private const val FLAG_FLOW = 1
    private const val FLAG_TRAIL = 2
    private const val FLAG_GLOW = 4
    private const val FLAG_CHROMATIC = 8
    private const val FLAG_SCANLINES = 16
    private const val ADDITIVE_FLAGS = FLAG_FLOW or FLAG_TRAIL or FLAG_GLOW or FLAG_CHROMATIC or FLAG_SCANLINES

    private const val CHROMA_RGB = 0
    private const val CHROMA_GHOST = 1

    private const val LABEL = "${LiquidBounce.CLIENT_NAME} ItemChams"

    private val captureTarget = LazyRenderTargetHolder("$LABEL Capture", useDepth = true)
    private val trailTargets = arrayOf(
        LazyRenderTargetHolder("$LABEL Trail 0", useDepth = false),
        LazyRenderTargetHolder("$LABEL Trail 1", useDepth = false),
    )

    private val linearSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
    private val nearestSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)

    private val cachedUniform = CachedUniform<EffectsUniform>(
        ClientUniformDefine.ITEM_CHAMS_EFFECTS,
    ) { value ->
        putFloat(value.time)
        putInt(value.flags)

        putVec4(value.flowColor)
        putFloat(value.flowSpeed)
        putFloat(value.flowScale)
        putFloat(value.flowSharpness)
        putFloat(value.flowIntensity)
        putFloat(value.flowDirX)
        putFloat(value.flowDirY)

        putVec4(value.trailColor)
        putFloat(value.trailWeight)
        putFloat(value.trailDecay)
        putFloat(value.trailDriftX)
        putFloat(value.trailDriftY)

        putVec4(value.glowColor)
        putFloat(value.glowRadius)
        putFloat(value.glowThickness)
        putFloat(value.glowIntensity)

        putVec4(value.scanColor)
        putFloat(value.scanSpacing)
        putFloat(value.scanThickness)
        putFloat(value.scanSpeed)
        putFloat(value.scanIntensity)
        putFloat(value.scanDirX)
        putFloat(value.scanDirY)

        putFloat(value.chromaStrength)
        putInt(value.chromaMode)

        putFloat(value.hueShift)
        putFloat(value.hueMix)
        putFloat(value.saturation)
        putFloat(value.brightness)
        putFloat(value.pulseSpeed)
        putFloat(value.pulseAmount)
    }

    private var captured = false
    private var trailIndex = 0
    private var trailFresh = false

    /**
     * Draws the first person hands and items of [frame] into the capture target.
     *
     * Must run while no render pass is open, i.e. before the vanilla "Item in hand" pass is created.
     */
    @JvmStatic
    fun capture(frame: FeatureRenderDispatcher.PreparedFrame) {
        if (!ModuleItemChams.Effects.running || frame.isEmpty) {
            return
        }

        captureTarget.initAndGet().createRenderPass(
            { "$LABEL capture" },
            clearColor = Optional.of(Vector4f(0f, 0f, 0f, 0f)),
            clearDepth = OptionalDouble.of(0.0),
        ).use { pass ->
            RenderSystem.bindDefaultUniforms(pass)
            FeatureRenderDispatcher.renderAllFeatures(pass, frame)
        }

        captured = true
    }

    /**
     * Composes the effect layer of the frame captured by [capture] into [target].
     */
    @JvmStatic
    fun compositeIfNeeded(target: RenderTarget) {
        if (!captured) {
            trailFresh = false
            return
        }
        captured = false

        if (!ModuleItemChams.Effects.running) {
            return
        }

        val source = captureTarget.get()
        val sourceColor = source?.colorTextureView
        val sourceDepth = source?.depthTextureView
        if (sourceColor == null || sourceDepth == null) {
            return
        }

        val recolor = ModuleItemChams.Effects.Recolor.running
        val withTrail = ModuleItemChams.Effects.Trail.running && prepareTrailTargets()
        val data = uniformData(Util.getMillis() / 1000f, withTrail)
        val additive = data.flags and ADDITIVE_FLAGS != 0
        if (!additive && !recolor) {
            return
        }

        val slice = cachedUniform.get(data)
        val trailView = if (withTrail) recordTrailPass(slice, sourceColor, sourceDepth) else null

        target.createRenderPass({ "$LABEL effects" }, useDepthAttachment = false).use { pass ->
            if (additive) {
                pass.setPipeline(ClientRenderPipelines.ItemChamsEffects)
                pass.setUniform("capture", sourceColor, linearSampler)
                pass.setUniform("depthTex", sourceDepth, nearestSampler)
                pass.setUniform("trail", trailView ?: sourceColor, linearSampler)
                pass.setUniform(ClientUniformDefine.ITEM_CHAMS_EFFECTS.uboName, slice)
                pass.draw(3, 1, 0, 0)
            }

            if (recolor) {
                pass.setPipeline(ClientRenderPipelines.ItemChamsRecolor)
                pass.setUniform("capture", sourceColor, linearSampler)
                pass.setUniform("depthTex", sourceDepth, nearestSampler)
                pass.setUniform(ClientUniformDefine.ITEM_CHAMS_EFFECTS.uboName, slice)
                pass.draw(3, 1, 0, 0)
            }
        }
    }

    @JvmStatic
    fun reset() {
        captured = false
        trailFresh = false
        trailIndex = 0
        captureTarget.close()
        trailTargets.forEach { it.close() }
    }

    /**
     * Allocates the trail history buffers at the current window size, invalidating the accumulated
     * trail whenever they had to be resized.
     */
    private fun prepareTrailTargets(): Boolean {
        val width = mc.window.width
        val height = mc.window.height
        if (width <= 0 || height <= 0) {
            return false
        }

        val history = trailTargets[0].get()
        if (history == null || history.width != width || history.height != height) {
            trailTargets[0].initAndGet(width, height)
            trailTargets[1].initAndGet(width, height)
            trailFresh = false
        }

        return true
    }

    /**
     * Accumulates the captured item into the history buffer and returns the accumulated view.
     *
     * Both buffers are cleared whenever the previous frame did not contribute (module or effect
     * toggled, third person, resize), so stale ghosts never pop up.
     */
    private fun recordTrailPass(
        slice: GpuBufferSlice,
        sourceColor: GpuTextureView,
        sourceDepth: GpuTextureView,
    ): GpuTextureView? {
        val history = trailTargets[trailIndex].get() ?: return null
        val output = trailTargets[1 - trailIndex].get() ?: return null
        val historyView = history.colorTextureView ?: return null
        val outputView = output.colorTextureView ?: return null

        if (!trailFresh) {
            history.clearColorAndDepth()
            output.clearColorAndDepth()
            trailFresh = true
        }

        output.createRenderPass({ "$LABEL trail" }, useDepthAttachment = false).use { pass ->
            pass.setPipeline(ClientRenderPipelines.ItemChamsTrail)
            pass.setUniform("capture", sourceColor, linearSampler)
            pass.setUniform("depthTex", sourceDepth, nearestSampler)
            pass.setUniform("history", historyView, linearSampler)
            pass.setUniform(ClientUniformDefine.ITEM_CHAMS_EFFECTS.uboName, slice)
            pass.draw(3, 1, 0, 0)
        }

        trailIndex = 1 - trailIndex
        return outputView
    }

    private fun uniformData(time: Float, withTrail: Boolean): EffectsUniform {
        val flow = ModuleItemChams.Effects.Flow
        val trail = ModuleItemChams.Effects.Trail
        val glow = ModuleItemChams.Effects.Glow
        val chromatic = ModuleItemChams.Effects.Chromatic
        val scanlines = ModuleItemChams.Effects.Scanlines
        val recolor = ModuleItemChams.Effects.Recolor

        var flags = 0
        if (flow.running) flags = flags or FLAG_FLOW
        if (withTrail) flags = flags or FLAG_TRAIL
        if (glow.running) flags = flags or FLAG_GLOW
        if (chromatic.running) flags = flags or FLAG_CHROMATIC
        if (scanlines.running) flags = flags or FLAG_SCANLINES

        val flowRadians = flow.angle.toRadians()
        val scanRadians = scanlines.angle.toRadians()

        return EffectsUniform(
            time = time,
            flags = flags,
            flowColor = flow.color,
            flowSpeed = flow.speed,
            flowScale = flow.scale,
            flowSharpness = flow.sharpness,
            flowIntensity = flow.intensity,
            flowDirX = cos(flowRadians),
            flowDirY = sin(flowRadians),
            trailColor = trail.color,
            trailWeight = trail.weight,
            trailDecay = trail.decay,
            trailDriftX = trail.drift.x(),
            trailDriftY = trail.drift.y(),
            glowColor = glow.color,
            glowRadius = glow.radius,
            glowThickness = glow.thickness,
            glowIntensity = glow.intensity,
            scanColor = scanlines.color,
            scanSpacing = scanlines.spacing,
            scanThickness = scanlines.thickness,
            scanSpeed = scanlines.speed,
            scanIntensity = scanlines.intensity,
            scanDirX = cos(scanRadians),
            scanDirY = sin(scanRadians),
            chromaStrength = chromatic.strength,
            chromaMode = if (chromatic.ghost) CHROMA_GHOST else CHROMA_RGB,
            hueShift = (time * recolor.hueSpeed).mod(1f),
            hueMix = recolor.hueMix,
            saturation = recolor.saturation,
            brightness = recolor.brightness,
            pulseSpeed = recolor.pulseSpeed,
            pulseAmount = recolor.pulseAmount,
        )
    }

    @Suppress("LongParameterList")
    private data class EffectsUniform(
        val time: Float,
        val flags: Int,
        val flowColor: Color4b,
        val flowSpeed: Float,
        val flowScale: Float,
        val flowSharpness: Float,
        val flowIntensity: Float,
        val flowDirX: Float,
        val flowDirY: Float,
        val trailColor: Color4b,
        val trailWeight: Float,
        val trailDecay: Float,
        val trailDriftX: Float,
        val trailDriftY: Float,
        val glowColor: Color4b,
        val glowRadius: Float,
        val glowThickness: Float,
        val glowIntensity: Float,
        val scanColor: Color4b,
        val scanSpacing: Float,
        val scanThickness: Float,
        val scanSpeed: Float,
        val scanIntensity: Float,
        val scanDirX: Float,
        val scanDirY: Float,
        val chromaStrength: Float,
        val chromaMode: Int,
        val hueShift: Float,
        val hueMix: Float,
        val saturation: Float,
        val brightness: Float,
        val pulseSpeed: Float,
        val pulseAmount: Float,
    )

}
