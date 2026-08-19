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
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.fastutil.enumSetAllOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.utils.TextureMode
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.PresetTexture
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.SecondaryPresetTexture
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.glow
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXLingering.SecondEffects.Effect.secondaryTextureMode
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.world.entityGetter
import net.ccbluex.liquidbounce.utils.world.filterTo
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.util.ARGB
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext

object PotionFXLingering : ToggleableValueGroup(ModulePotionFX, "LingeringPotion", false) {

    private object MainEffect : ValueGroup("MainEffect") {
        val extraRadius by float("ExtraRadius", 0.375f, 0f..10f)
        val rotationSpeed by float("RotationSpeed", 1f, -10f..10f)

        val textureMode = modes(this@PotionFXLingering, "Source", 0) {
            arrayOf(
                TextureMode.Builtin(it, PresetTexture.DASHED, enumSetAllOf<PresetTexture>()),
                TextureMode.Custom(it),
            )
        }
    }

    private object SecondEffects : ValueGroup("SecondEffects") {
        object Flash : ToggleableValueGroup(this@PotionFXLingering, "Flash", true) {
            val animTime by int("AnimTime", 4, 1..20)
            val radius by float("Radius", 2f, 0.1f..10f)
        }

        val flash = tree(Flash)

        object Effect : ToggleableValueGroup(this@PotionFXLingering, "Effect", false) {
            val rotationSpeed by float("RotationSpeed", 1f, -10f..10f)
            val extraRadius by float("ExtraRadius", 0f, 0f..10f)
            val secondaryTextureMode = modes(this, "Source", 0) {
                arrayOf(
                    TextureMode.Builtin(it, SecondaryPresetTexture.CRACKED, enumSetAllOf<SecondaryPresetTexture>()),
                    TextureMode.Custom(it),
                )
            }
        }

        val effect = tree(Effect)
    }

    init {
        tree(MainEffect)
        tree(SecondEffects)
    }

    private val canBeCovered by boolean("CanBeCovered", true)

    @Suppress("unused")
    private val splashHandler = handler<PacketEvent> { event ->
        if (event.packet !is ClientboundLevelEventPacket || event.packet.type != 2002) return@handler

        world.getEntities(EntityTypes.LINGERING_POTION, AABB(event.packet.pos).inflate(3.0)) { true }
            .minByOrNull { it.distanceToSqr(Vec3.atCenterOf(event.packet.pos)) } ?: return@handler

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
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {

            val texture = MainEffect.textureMode.activeMode.texture ?: return@handler
            val secondaryTexture = secondaryTextureMode.activeMode.texture ?: return@handler
            if (cloudEntities.isEmpty()) return@handler

            for (entity in cloudEntities) {

                val age = 40 - (40 - entity.tickCount).coerceIn(0, 40) + event.partialTicks
                val glowProgress = Easing.QUAD_IN_OUT
                    .transform(age / SecondEffects.Flash.animTime)
                    .coerceIn(0f, 1f)

                val rotation = (entity.tickCount + event.partialTicks) * MainEffect.rotationSpeed
                val secondRotation =
                    (entity.tickCount + event.partialTicks) * SecondEffects.Effect.rotationSpeed

                val color = when (val particle = entity.particle) {
                    is ColorParticleOption -> ARGB.color(
                        (particle.red * 255).toInt(),
                        (particle.green * 255).toInt(),
                        (particle.blue * 255).toInt()
                    )

                    else -> Color4b.LIQUID_BOUNCE.argb
                }

                withPositionRelativeToCamera(entity.position().add(0.0, 0.011, 0.0)) {
                    poseStack.withPush {
                        withPush {
                            mulPose(Axis.XP.rotationDegrees(-90f))
                            mulPose(Axis.ZP.rotationDegrees(rotation))
                            drawSquareTexture(
                                texture,
                                (entity.radius + MainEffect.extraRadius) * 2,
                                color,
                                AnchorPoint.CENTER,
                                noDepthTest = !canBeCovered
                            )
                        }
                        if (SecondEffects.flash.enabled) {
                            withPush {
                                mulPose(mc.gameRenderer.mainCamera().rotation())
                                drawSquareTexture(
                                    glow,
                                    SecondEffects.flash.radius * 2 * glowProgress,
                                    color,
                                    AnchorPoint.CENTER,
                                    noDepthTest = !canBeCovered
                                )
                            }
                        }
                    }
                }

                withPositionRelativeToCamera(entity.position().add(0.0, 0.01, 0.0)) {
                    poseStack.withPush {
                        if (SecondEffects.effect.enabled) {
                            mulPose(Axis.XP.rotationDegrees(-90f))
                            mulPose(Axis.ZP.rotationDegrees(secondRotation))
                            drawSquareTexture(
                                secondaryTexture,
                                (entity.radius + SecondEffects.effect.extraRadius + MainEffect.extraRadius) * 2,
                                color,
                                AnchorPoint.CENTER,
                                noDepthTest = !canBeCovered
                            )
                        }
                    }
                }
            }
        }
    }

    private val cloudEntities by computedOn<GameTickEvent, MutableSet<AreaEffectCloud>>(ReferenceOpenHashSet())
    { _, set ->
        set.clear()
        if (!enabled) return@computedOn set
        world.entityGetter.filterTo(set, EntityTypes.AREA_EFFECT_CLOUD) { true }
        set
    }

    override fun onDisabled() {
        cloudEntities.clear()
        super.onDisabled()
    }

}
