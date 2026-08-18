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
import net.ccbluex.liquidbounce.config.utils.TextureMode
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.PresetTexture
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext

object PotionFXSplash : ToggleableValueGroup(ModulePotionFX, "SplashPotion", false) {

    private val radius by float("Radius", 2f, 0.1f..10f)
    private val rotationSpeed by float("RotationSpeed", 0f, -10f..10f)
    private val lifetime by intRange("lifetime", 10..40, 1..1000)
    private val fadeStart by float("fadeStart", 0.9f, 0.01f..1f)
    private val canBeCovered by boolean("CanBeCovered", true)

    private val textureMode = modes(this, "Source", 0) {
        arrayOf(
            TextureMode.Builtin(it, PresetTexture.SIMPLE, enumSetAllOf<PresetTexture>()),
            TextureMode.Custom(it),
        )
    }

    private val splashes = ExpiringList<SplashData>()

    @Suppress("unused")
    private val splashHandler = handler<PacketEvent> { event ->
        if (event.packet !is ClientboundLevelEventPacket || event.packet.type != 2002) return@handler

        val nearestPotion = world.getEntities(
            EntityTypeTest.forClass(ThrowableItemProjectile::class.java),
            AABB(event.packet.pos).inflate(3.0)
        ) { true }.minByOrNull { it.distanceToSqr(Vec3.atCenterOf(event.packet.pos)) } ?: return@handler

        if (nearestPotion.item.`is`(Items.LINGERING_POTION)) return@handler

        val packetOrigin = Vec3.atCenterOf(event.packet.pos)

        val pos = world.clip(
            ClipContext(
                packetOrigin,
                packetOrigin.add(0.0, -1.0, 0.0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
            )
        ).let { if (it.type == HitResult.Type.BLOCK) it.location else packetOrigin }

        splashes.add(SplashData(event.packet.data, pos), lifetime.last)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {

            val texture = textureMode.activeMode.texture ?: return@handler

            for (splash in splashes) {

                withPositionRelativeToCamera(splash.value.pos.add(0.0, 0.01, 0.0)) {
                    poseStack.withPush {
                        val age = lifetime.last - splashes.timeToDie(splash) + event.partialTicks
                        val progress = Easing.EXPONENTIAL_OUT
                            .transform(age / lifetime.first)
                            .coerceIn(0f, 1f)

                        // fucking mojang
                        val alpha = (splash.value.color ushr 24) and 0xFF
                        val red = (splash.value.color ushr 16) and 0xFF
                        val green = (splash.value.color ushr 8) and 0xFF
                        val blue = splash.value.color and 0xFF

                        val fade = (
                                (splashes.timeToDie(splash) - event.partialTicks) / (lifetime.last * (1.0f - fadeStart))
                            ).coerceIn(0f, 1f)

                        val newAlpha = (alpha * fade).toInt().coerceIn(0, 255)
                        val animatedColor = (newAlpha shl 24) or (red shl 16) or (green shl 8) or blue

                        val currentRotation = age * rotationSpeed

                        mulPose(Axis.XP.rotationDegrees(-90f))
                        mulPose(Axis.ZP.rotationDegrees(currentRotation))
                        drawSquareTexture(
                            texture,
                            radius * 2 * progress,
                            animatedColor,
                            AnchorPoint.CENTER,
                            noDepthTest = !canBeCovered
                        )
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
