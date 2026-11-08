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
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import net.ccbluex.liquidbounce.common.LightmapOverride
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinGameRenderer
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.ItemChamsEffectRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.setPipeline
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.ccbluex.liquidbounce.utils.render.ShieldTintContext
import net.ccbluex.liquidbounce.utils.render.asView
import net.ccbluex.liquidbounce.utils.render.putVec4
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.ARGB
import net.minecraft.world.entity.LivingEntity
import org.joml.Vector2f
import java.util.function.Supplier

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

    private val lightAccent = Color4b.LIQUID_BOUNCE.interpolateTo(Color4b.WHITE, 0.3F)

    object Lightmap : ToggleableValueGroup(this, "Lightmap", true) {
        private val blendColor by color("BlendColor", Color4b(0, 64, 255, 186)).markDirtyOnChanged()
        private val alpha by int("Alpha", 95, 1..255).markDirtyOnChanged()
        private val glowColor by color("GlowColor", Color4b(0, 64, 255, 15)).markDirtyOnChanged()
        private val layers by int("Layers", 3, 1..10).markDirtyOnChanged()
        private val layerSize by float("LayerSize", 1.91f, 1f..5f).markDirtyOnChanged()
        private val falloff by float("Falloff", 6.83f, 0f..20f).markDirtyOnChanged()

        private var texture: GpuTexture? = null
        private var textureView: GpuTextureView? = null

        private val UBO = ClientUniformDefine.HAND_ITEM_LIGHTMAP.createSingleBuffer()

        private var uboDirty = true
        private fun <T : Any> Value<T>.markDirtyOnChanged() = onChanged { uboDirty = true }

        private val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, false)

        @JvmStatic
        fun <T : Any> doOverride(op: Supplier<T>): T {
            if (!this.running || this.textureView == null) return op.get()
            return LightmapOverride.call(this.textureView!!, op)
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
                this.texture = gpuDevice.createTexture(
                    "$name - Lightmap Texture",
                    GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                    GpuFormat.RGBA8_UNORM, 16, 16, 1, 1,
                ).also { this.textureView = it.asView() }
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
            texture?.close()
            texture = null
            textureView?.close()
            textureView = null
            super.onDisabled()
        }

    }

    /**
     * Tints shields, your own and with [others] those of other players, also by their [ShieldState].
     */
    object Shield : ToggleableValueGroup(this, "Shield", true) {
        private val tintMode by enumChoice("TintMode", ShieldTintMode.MULTIPLY)
        private val tint by color("Tint", Color4b.WHITE)
        private val self by boolean("Self", true)
        private val others by boolean("Others", false)

        private object States : ToggleableValueGroup(Shield, "States", false) {
            val blocking by color("Blocking", Color4b(0, 160, 255))
            val disabled by color("Disabled", Color4b(255, 60, 60))
        }

        init {
            tree(States)
        }

        /**
         * Game time until which a shield of another player stays disabled, by entity id. Only the own
         * cooldowns are known and the game no longer sends an entity event for a broken shield, so an axe hit
         * is told by the shield break sound at the player.
         */
        private val disabledUntil = Int2LongOpenHashMap()

        private const val DISABLE_TICKS = 100
        private const val SOUND_RANGE_SQ = 2.25

        fun stateOf(entity: LivingEntity): ShieldState {
            val disabled = if (entity is LocalPlayer) {
                entity.handItems.any { it.has(DataComponents.BLOCKS_ATTACKS) && entity.cooldowns.isOnCooldown(it) }
            } else {
                disabledUntil.get(entity.id) > entity.level().gameTime
            }

            return when {
                disabled -> ShieldState.DISABLED
                entity.isBlocking -> ShieldState.BLOCKING
                else -> ShieldState.IDLE
            }
        }

        /**
         * Tints the colour [tintedColor] of the shield [ShieldTintContext] renders, if any.
         */
        @JvmStatic
        fun applyTint(tintedColor: Int): Int {
            val holder = ShieldTintContext.holder()
            if (!running || holder == null || !(if (holder is LocalPlayer) self else others)) {
                return tintedColor
            }

            val color = if (States.running) {
                when (stateOf(holder)) {
                    ShieldState.BLOCKING -> States.blocking
                    ShieldState.DISABLED -> States.disabled
                    ShieldState.IDLE -> tint
                }
            } else {
                tint
            }

            return when (tintMode) {
                ShieldTintMode.OVERRIDE -> color.argb
                ShieldTintMode.MULTIPLY -> ARGB.multiply(tintedColor, color.argb)
            }
        }

        @JvmStatic
        fun usesTranslucentTint(tintedColor: Int) = ARGB.alpha(applyTint(tintedColor)) < 255

        @Suppress("unused")
        private val soundHandler = handler<PacketEvent> { event ->
            val packet = event.packet as? ClientboundSoundPacket ?: return@handler
            if (!packet.sound.`is`(SoundEvents.SHIELD_BREAK)) {
                return@handler
            }

            mc.execute {
                val level = mc.level ?: return@execute
                val holder = level.players()
                    .filter { it !== mc.player }
                    .minByOrNull { it.distanceToSqr(packet.x, packet.y, packet.z) }
                    ?.takeIf { it.distanceToSqr(packet.x, packet.y, packet.z) < SOUND_RANGE_SQ }
                    ?: return@execute
                disabledUntil.put(holder.id, level.gameTime + DISABLE_TICKS)
            }
        }

        @Suppress("unused")
        private val worldChangeHandler = handler<WorldChangeEvent> {
            disabledUntil.clear()
        }
    }

    enum class ShieldState {
        IDLE,
        BLOCKING,

        /**
         * Knocked out of blocking by an axe, until the cooldown ends.
         */
        DISABLED,
    }

    /**
     * Screen space layers composed on top of the held item, see [ItemChamsEffectRenderer].
     */
    object Effects : ToggleableValueGroup(this, "Effects", false) {

        object Flow : ToggleableValueGroup(Effects, "Flow", true) {
            val color by color("Color", lightAccent)
            val speed by float("Speed", 0.6f, 0f..5f)
            val scale by float("Scale", 12f, 1f..64f)
            val sharpness by float("Sharpness", 6f, 1f..64f)
            val angle by float("Angle", 45f, 0f..360f, "°")
            val intensity by float("Intensity", 0.8f, 0f..4f)
        }

        object Trail : ToggleableValueGroup(Effects, "Trail", true) {
            val color by color("Color", Color4b.LIQUID_BOUNCE)
            val weight by float("Weight", 0.7f, 0f..4f)
            val decay by float("Decay", 0.86f, 0f..1f)
            val drift by vec2f("Drift", Vector2f(2f, 0f))
        }

        object Glow : ToggleableValueGroup(Effects, "Glow", false) {
            val color by color("Color", lightAccent)
            val radius by float("Radius", 6f, 1f..32f, "px")
            val thickness by float("Thickness", 1.5f, 0.2f..8f)
            val intensity by float("Intensity", 0.9f, 0f..4f)
        }

        object Chromatic : ToggleableValueGroup(Effects, "Chromatic", false) {
            val strength by float("Strength", 2f, 0f..16f, "px")
            val ghost by boolean("Ghost", false)
        }

        object Scanlines : ToggleableValueGroup(Effects, "Scanlines", false) {
            val color by color("Color", lightAccent)
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
