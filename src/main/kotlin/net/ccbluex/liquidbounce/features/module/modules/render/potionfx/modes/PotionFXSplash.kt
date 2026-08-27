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

package net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes

import com.mojang.math.Axis
import net.ccbluex.fastutil.enumSetAllOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.utils.TextureMode
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.PresetTexture
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.SecondaryPresetTexture
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.glow
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXSplash.MainEffect.fadeStart
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXSplash.MainEffect.lifetime
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXSplash.MainEffect.radius
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXSplash.SecondEffects.Effect.animAcceleration
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXSplash.SecondEffects.Effect.extraRadius
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXSplash.SecondEffects.Effect.secondaryTextureMode
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.util.ARGB
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.LevelEvent.PARTICLES_SPELL_POTION_SPLASH
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext

object PotionFXSplash : ToggleableValueGroup(ModulePotionFX, "SplashPotion", true) {

    private object MainEffect : ValueGroup("MainEffect") {
        val lifetime by intRange("Lifetime", 20..40, 1..1000)
        val radius by float("Radius", 3f, 0.1f..10f)
        val rotationSpeed by float("RotationSpeed", 3f, -10f..10f)
        val fadeStart by float("FadeStart", 0.9f, 0.01f..1f)

        val textureMode = modes(this@PotionFXSplash, "Source", 0) {
            arrayOf(
                TextureMode.Builtin(it, PresetTexture.DASHED, enumSetAllOf<PresetTexture>()),
                TextureMode.Custom(it),
            )
        }
    }

    private object SecondEffects : ValueGroup("SecondEffects") {
        object Flash : ToggleableValueGroup(this@PotionFXSplash, "Flash", true) {
            val animTime by int("AnimTime", 4, 1..20)
            val radius by float("Radius", 2f, 0.1f..10f)
        }

        object Effect : ToggleableValueGroup(this@PotionFXSplash, "Effect", false) {
            val animAcceleration by float("AnimAcceleration", 1.25f, 0.1f..2f)
            val rotationSpeed by float("RotationSpeed", 3f, -10f..10f)
            val extraRadius by float("ExtraRadius", 0f, 0f..10f)
            val secondaryTextureMode = modes(this, "Source", 0) {
                arrayOf(
                    TextureMode.Builtin(it, SecondaryPresetTexture.CRACKED, enumSetAllOf<SecondaryPresetTexture>()),
                    TextureMode.Custom(it),
                )
            }
        }

        init {
            tree(Flash)
            tree(Effect)
        }
    }

    init {
        tree(MainEffect)
        tree(SecondEffects)
    }

    private val canBeCovered by boolean("CanBeCovered", true)

    private val splashes = ExpiringList<SplashData>()

    override fun onDisabled() {
        splashes.clear()
        super.onDisabled()
    }

    @Suppress("unused")
    private val splashHandler = handler<PacketEvent> { event ->
        if (event.packet !is ClientboundLevelEventPacket
            || event.packet.type != PARTICLES_SPELL_POTION_SPLASH) { return@handler }

        val world = mc.level ?: return@handler
        world.getEntities(EntityTypes.SPLASH_POTION, AABB(event.packet.pos).inflate(3.0)) { true }
            .ifEmpty { return@handler }

        mc.execute {
            val packetPos = Vec3.atCenterOf(event.packet.pos)
            val pos = world.clip(
                ClipContext(
                    packetPos,
                    packetPos.add(0.0, -1.0, 0.0),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
                )
            ).let { if (it.type == HitResult.Type.BLOCK) it.location else packetPos }

            splashes.add(SplashData(event.packet.data, pos), lifetime.last)
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            val texture = MainEffect.textureMode.activeMode.texture ?: return@handler
            val secondaryTexture = secondaryTextureMode.activeMode.texture ?: return@handler

            for (splash in splashes) {
                val timeToDie = splashes.timeToDie(splash)

                val age = lifetime.last - timeToDie + event.partialTicks
                val progress = Easing.EXPONENTIAL_OUT
                    .transform(age / lifetime.first)
                    .coerceIn(0f, 1f)
                val glowProgress = Easing.QUAD_IN_OUT
                    .transform(age / SecondEffects.Flash.animTime)
                    .coerceIn(0f, 1f)

                val fade =
                    ((timeToDie - event.partialTicks) / (lifetime.last * (1.0f - fadeStart))).coerceIn(0f, 1f)

                val alpha = (ARGB.alpha(splash.value.color) * fade).toInt().coerceIn(0, 255)
                val animatedColor = ARGB.color(alpha, splash.value.color)

                withPositionRelativeToCamera(splash.value.pos.add(0.0, 0.01, 0.0)) {
                    poseStack.withPush {
                        withPush {
                            mulPose(Axis.XP.rotationDegrees(-90f))
                            mulPose(Axis.ZP.rotationDegrees(age * MainEffect.rotationSpeed))
                            drawSquareTexture(
                                texture,
                                radius * 2 * progress,
                                animatedColor,
                                AnchorPoint.CENTER,
                                noDepthTest = !canBeCovered
                            )
                        }
                        if (SecondEffects.Effect.enabled) {
                            withPush {
                                translate(0.0, -0.005, 0.0)
                                mulPose(Axis.XP.rotationDegrees(-90f))
                                mulPose(Axis.ZP.rotationDegrees(age * SecondEffects.Effect.rotationSpeed))
                                drawSquareTexture(
                                    secondaryTexture,
                                    (radius + extraRadius) * 2 * (progress * animAcceleration).coerceIn(0f, 1f),
                                    animatedColor,
                                    AnchorPoint.CENTER,
                                    noDepthTest = !canBeCovered
                                )
                            }
                        }
                        if (SecondEffects.Flash.enabled) {
                            withPush {
                                mulPose(mc.gameRenderer.mainCamera().rotation())
                                drawSquareTexture(
                                    glow,
                                    SecondEffects.Flash.radius * 2 * glowProgress,
                                    splash.value.color,
                                    AnchorPoint.CENTER,
                                    noDepthTest = !canBeCovered
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private data class SplashData(
        val color: Int,
        val pos: Vec3,
    )
}
