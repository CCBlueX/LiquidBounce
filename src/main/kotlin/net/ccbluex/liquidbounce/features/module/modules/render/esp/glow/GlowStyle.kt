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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode
import net.ccbluex.liquidbounce.render.ClientShaders
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.minecraft.client.renderer.UniformValue
import net.minecraft.resources.Identifier
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.sin

sealed class GlowStyle(name: String) : Mode(name) {
    final override val parent: ModeValueGroup<*>
        get() = EspGlowMode.styles
}

/**
 * The game's own entity outline.
 */
object VanillaGlow : GlowStyle("Vanilla")

/**
 * A style drawn by [GlowPostEffect] from the distance of every pixel to the glowing silhouettes.
 */
sealed class ShaderGlow(name: String, shader: Identifier) : GlowStyle(name) {

    private val opacity by float("Opacity", 1f, 0f..1f)

    val pipeline = GlowPostEffect.pipeline(shader)

    /**
     * How far from the silhouettes the style draws, in pixels.
     */
    protected abstract val range: Float

    protected open val accent: Color4b
        get() = Color4b.WHITE

    /**
     * `Params0` and `Params1` of the `EspGlowData` block, missing ones are 0.
     */
    protected abstract fun params(): FloatArray

    fun uniforms(): List<UniformValue> {
        val params = params().copyOf(PARAMS)
        return listOf(
            UniformValue.Vec4Uniform(Vector4f(params[0], params[1], params[2], params[3])),
            UniformValue.Vec4Uniform(Vector4f(params[4], params[5], params[6], params[7])),
            UniformValue.Vec4Uniform(accent.toVector4f()),
            UniformValue.FloatUniform(range.coerceIn(1f, MAX_RANGE)),
            UniformValue.FloatUniform(opacity),
        )
    }

    private companion object {
        const val PARAMS = 8

        // Diagonal distances have to stay below FIELD_LIMIT of glow.glsl
        const val MAX_RANGE = 64f
    }

}

object SmoothGlow : ShaderGlow("Smooth", ClientShaders.Fragment.EspGlowSmooth) {
    private val radius by float("Radius", 6f, 1f..32f, "px")
    private val falloff by float("Falloff", 1.6f, 0.5f..4f)
    private val intensity by float("Intensity", 1f, 0f..2f)
    private val fill by float("Fill", 0.12f, 0f..1f)

    override val range get() = radius

    override fun params() = floatArrayOf(radius, falloff, intensity, fill)
}

object OutlineGlow : ShaderGlow("Outline", ClientShaders.Fragment.EspGlowOutline) {
    private val width by float("Width", 2f, 0.5f..8f, "px")
    private val softness by float("Softness", 1f, 0.1f..4f, "px")
    private val glowRadius by float("GlowRadius", 6f, 0f..24f, "px")
    private val glowIntensity by float("GlowIntensity", 0.35f, 0f..1f)
    private val fill by float("Fill", 0f, 0f..1f)

    override val range get() = width + glowRadius + softness

    override fun params() = floatArrayOf(width, softness, glowRadius, glowIntensity, fill)
}

object NeonGlow : ShaderGlow("Neon", ClientShaders.Fragment.EspGlowNeon) {
    private val coreColor by color("CoreColor", Color4b.WHITE)
    private val coreMix by float("CoreMix", 0.7f, 0f..1f)
    private val coreWidth by float("CoreWidth", 1.5f, 0.5f..6f, "px")
    private val bloomRadius by float("BloomRadius", 12f, 1f..32f, "px")
    private val bloomIntensity by float("BloomIntensity", 1.2f, 0f..3f)
    private val fill by float("Fill", 0.08f, 0f..1f)
    private val pulseSpeed by float("PulseSpeed", 0.5f, 0f..4f, "Hz")
    private val pulseAmount by float("PulseAmount", 0.25f, 0f..1f)

    override val range get() = maxOf(coreWidth, bloomRadius * 2f)

    override val accent get() = coreColor

    override fun params() = floatArrayOf(
        coreWidth, bloomRadius, bloomIntensity, fill,
        coreMix, pulseSpeed, pulseAmount,
    )
}

object RainbowGlow : ShaderGlow("Rainbow", ClientShaders.Fragment.EspGlowRainbow) {
    private val width by float("Width", 2f, 0.5f..8f, "px")
    private val glowRadius by float("GlowRadius", 6f, 0f..24f, "px")
    private val glowIntensity by float("GlowIntensity", 0.5f, 0f..1f)
    private val fill by float("Fill", 0.1f, 0f..1f)
    private val speed by float("Speed", 0.35f, -2f..2f)
    private val spread by float("Spread", 400f, 50f..2000f, "px")
    private val saturation by float("Saturation", 0.85f, 0f..1f)
    private val entityMix by float("EntityMix", 0f, 0f..1f)

    override val range get() = width + glowRadius

    override fun params() = floatArrayOf(
        width, glowRadius, glowIntensity, fill,
        speed, spread, saturation, entityMix,
    )
}

object RippleGlow : ShaderGlow("Ripple", ClientShaders.Fragment.EspGlowRipple) {
    private val reach by float("Reach", 28f, 4f..64f, "px")
    private val spacing by float("Spacing", 10f, 3f..32f, "px")
    private val ringWidth by float("RingWidth", 2f, 0.5f..8f, "px")
    private val speed by float("Speed", 16f, -64f..64f, "px/s")
    private val edgeWidth by float("EdgeWidth", 1.5f, 0f..6f, "px")
    private val fill by float("Fill", 0.08f, 0f..1f)

    override val range get() = reach

    override fun params() = floatArrayOf(spacing, ringWidth, speed, edgeWidth, fill)
}

object HologramGlow : ShaderGlow("Hologram", ClientShaders.Fragment.EspGlowHologram) {
    private val stripeColor by color("StripeColor", Color4b.WHITE)
    private val stripes by float("Stripes", 0.55f, 0f..1f)
    private val spacing by float("Spacing", 6f, 2f..32f, "px")
    private val angle by float("Angle", 90f, 0f..180f, "°")
    private val speed by float("Speed", 1f, -8f..8f)
    private val fill by float("Fill", 0.18f, 0f..1f)
    private val edgeWidth by float("EdgeWidth", 1.5f, 0.5f..6f, "px")
    private val flicker by float("Flicker", 0.15f, 0f..1f)

    override val range get() = edgeWidth + 8f

    override val accent get() = stripeColor

    override fun params(): FloatArray {
        val radians = angle.toRadians()
        return floatArrayOf(
            spacing, cos(radians), sin(radians), speed,
            stripes, fill, edgeWidth, flicker,
        )
    }
}

object HalftoneGlow : ShaderGlow("Halftone", ClientShaders.Fragment.EspGlowHalftone) {
    private val reach by float("Reach", 12f, 2f..48f, "px")
    private val cellSize by float("CellSize", 5f, 3f..24f, "px")
    private val dotScale by float("DotScale", 0.9f, 0.2f..1.5f)
    private val fill by float("Fill", 0f, 0f..1f)
    private val edgeWidth by float("EdgeWidth", 1f, 0f..4f, "px")

    override val range get() = reach + cellSize

    override fun params() = floatArrayOf(cellSize, dotScale, fill, edgeWidth)
}

object ChromaticGlow : ShaderGlow("Chromatic", ClientShaders.Fragment.EspGlowChromatic) {
    private val width by float("Width", 2f, 0.5f..8f, "px")
    private val split by float("Split", 3f, 0f..12f, "px")
    private val speed by float("Speed", 1.5f, -8f..8f, "rad/s")
    private val tint by float("Tint", 0.3f, 0f..1f)
    private val glowRadius by float("GlowRadius", 3f, 0f..16f, "px")
    private val fill by float("Fill", 0.08f, 0f..1f)

    override val range get() = width + glowRadius + split

    override fun params() = floatArrayOf(width, split, speed, tint, glowRadius, fill)
}

object AuraGlow : ShaderGlow("Aura", ClientShaders.Fragment.EspGlowAura) {
    private val radius by float("Radius", 6f, 2f..32f, "px")
    private val height by float("Height", 16f, 0f..48f, "px")
    private val sway by float("Sway", 3f, 0f..16f, "px")
    private val scale by float("Scale", 10f, 4f..64f, "px")
    private val speed by float("Speed", 1.5f, 0f..6f)
    private val intensity by float("Intensity", 1.2f, 0f..3f)
    private val heat by float("Heat", 0.3f, 0f..1f)
    private val fill by float("Fill", 0.1f, 0f..1f)

    override val range get() = radius + height + sway

    override fun params() = floatArrayOf(
        radius, height, sway, scale,
        speed, intensity, heat, fill,
    )
}
