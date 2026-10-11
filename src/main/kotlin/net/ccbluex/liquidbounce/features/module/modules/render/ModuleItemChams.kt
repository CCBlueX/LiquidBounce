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
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinGameRenderer
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.ItemChamsEffectRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.setPipeline
import net.ccbluex.liquidbounce.utils.render.asView
import net.ccbluex.liquidbounce.utils.render.putVec4
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.util.ARGB
import org.joml.Vector2f

/**
 * Module ItemChams
 *
 * Applies visual effects to your held items.
 *
 * @see MixinGameRenderer
 * @see net.minecraft.client.renderer.Lightmap
 *
 * @author ccetl
 */
object ModuleItemChams : ClientModule("ItemChams", ModuleCategories.RENDER) {

    object Lightmap : ToggleableValueGroup(this, "Lightmap", true) {
        private val blendColor by color("BlendColor", Color4b(0, 64, 255, 186)).markDirtyOnChanged()
        private val alpha by int("Alpha", 95, 1..255).markDirtyOnChanged()
        private val glowColor by color("GlowColor", Color4b(0, 64, 255, 15)).markDirtyOnChanged()
        private val layers by int("Layers", 3, 1..10).markDirtyOnChanged()
        private val layerSize by float("LayerSize", 1.91f, 1f..5f).markDirtyOnChanged()
        private val falloff by float("Falloff", 6.83f, 0f..20f).markDirtyOnChanged()

        @JvmField val OVERRIDE: ScopedValue<GpuTextureView> = ScopedValue.newInstance()

        private var textureView: GpuTextureView? = null

        private val UBO = ClientUniformDefine.HAND_ITEM_LIGHTMAP.createSingleBuffer()

        private var uboDirty = true
        private fun <T : Any> Value<T>.markDirtyOnChanged() = onChanged { uboDirty = true }

        private val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, false)

        @JvmStatic
        fun <T : Any> doOverride(op: ScopedValue.CallableOp<T, Throwable>): T {
            if (!this.running || this.textureView == null) return op.call()
            return ScopedValue.where(OVERRIDE, this.textureView).call(op)
        }

        /**
         * Regenerates the chams lightmap texture on top of the vanilla lightmap.
         *
         * Must be called when no render pass is open.
         *
         * @see net.minecraft.client.renderer.Lightmap
         */
        @JvmStatic
        fun refresh(vanillaLightmapView: GpuTextureView) {
            if (!this.running) return

            if (this.textureView == null) {
                this.textureView = gpuDevice.createTexture(
                    "$name - Lightmap Texture",
                    GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                    GpuFormat.RGBA8_UNORM, 16, 16, 1, 1,
                ).asView()
            }

            if (uboDirty) {
                UBO.writeStd140 {
                    putInt(0)
                    putFloat(alpha / 255f)
                    putVec4(blendColor)
                    putFloat(layerSize)
                    putVec4(glowColor)
                    putFloat(falloff)
                    putInt(layers)
                }
                uboDirty = false
            }

            textureView!!.createRenderPass({ "$name Pass" }).use { pass ->
                pass.setPipeline(ClientRenderPipelines.ItemChams)

                pass.setUniform("texture0", vanillaLightmapView, sampler)
                pass.setUniform("image", vanillaLightmapView, sampler)
                pass.setUniform(ClientUniformDefine.HAND_ITEM_LIGHTMAP.uboName, UBO)

                pass.draw(3, 1, 0, 0)
            }
        }

        override fun onDisabled() {
            uboDirty = true
            textureView?.close()
            textureView = null
            super.onDisabled()
        }

    }

    object Shield : ToggleableValueGroup(this, "Shield", true) {
        private val tintMode by enumChoice("TintMode", ShieldTintMode.MULTIPLY)
        private val tint by color("Tint", Color4b.WHITE)

        fun applyTint(tintedColor: Int): Int {
            if (!running) {
                return tintedColor
            }

            return when (tintMode) {
                ShieldTintMode.OVERRIDE -> tint.argb
                ShieldTintMode.MULTIPLY -> ARGB.multiply(tintedColor, tint.argb)
            }
        }

        fun usesTranslucentTint(tintedColor: Int) = running && ARGB.alpha(applyTint(tintedColor)) < 255
    }

    /**
     * Screen space layers composed on top of the held item, see [ItemChamsEffectRenderer].
     */
    object Effects : ToggleableValueGroup(this, "Effects", false) {

        object Flow : ToggleableValueGroup(Effects, "Flow", true) {
            val color by color("Color", Color4b(90, 170, 255))
            val speed by float("Speed", 0.6f, 0f..5f)
            val scale by float("Scale", 12f, 1f..64f)
            val sharpness by float("Sharpness", 6f, 1f..64f)
            val angle by float("Angle", 45f, 0f..360f, "°")
            val intensity by float("Intensity", 0.8f, 0f..4f)
        }

        object Trail : ToggleableValueGroup(Effects, "Trail", true) {
            val color by color("Color", Color4b(70, 130, 255))
            val weight by float("Weight", 0.7f, 0f..4f)
            val decay by float("Decay", 0.86f, 0f..1f)
            val drift by vec2f("Drift", Vector2f(2f, 0f))
        }

        object Glow : ToggleableValueGroup(Effects, "Glow", false) {
            val color by color("Color", Color4b(90, 170, 255))
            val radius by float("Radius", 6f, 1f..32f, "px")
            val thickness by float("Thickness", 1.5f, 0.2f..8f)
            val intensity by float("Intensity", 0.9f, 0f..4f)
        }

        object Chromatic : ToggleableValueGroup(Effects, "Chromatic", false) {
            val strength by float("Strength", 2f, 0f..16f, "px")
            val ghost by boolean("Ghost", false)
        }

        object Scanlines : ToggleableValueGroup(Effects, "Scanlines", false) {
            val color by color("Color", Color4b(90, 170, 255))
            val spacing by float("Spacing", 40f, 2f..256f, "px")
            val thickness by float("Thickness", 0.4f, 0.02f..1f)
            val speed by float("Speed", 0.3f, -5f..5f)
            val angle by float("Angle", 90f, 0f..360f, "°")
            val intensity by float("Intensity", 0.5f, 0f..4f)
        }

        object Recolor : ToggleableValueGroup(Effects, "Recolor", false) {
            val hueSpeed by float("HueSpeed", 0.2f, -2f..2f)
            val hueMix by float("HueMix", 1f, 0f..1f)
            val saturation by float("Saturation", 1.4f, 0f..3f)
            val brightness by float("Brightness", 1.2f, 0f..3f)
            val pulseSpeed by float("PulseSpeed", 2f, 0f..12f)
            val pulseAmount by float("PulseAmount", 0.15f, 0f..1f)
        }

        init {
            tree(Flow)
            tree(Trail)
            tree(Glow)
            tree(Chromatic)
            tree(Scanlines)
            tree(Recolor)
        }

        override fun onDisabled() {
            ItemChamsEffectRenderer.reset()
        }

    }

    init {
        tree(Lightmap)
        tree(Shield)
        tree(Effects)
    }

    override fun onDisabled() {
        ItemChamsEffectRenderer.reset()
    }

    private enum class ShieldTintMode(override val tag: String) : Tagged {
        OVERRIDE("Override"),
        MULTIPLY("Multiply"),
    }

}
