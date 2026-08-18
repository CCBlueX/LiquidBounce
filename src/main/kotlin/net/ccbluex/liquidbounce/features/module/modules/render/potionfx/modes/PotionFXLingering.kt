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
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.PresetTexture
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.item.alchemy.PotionContents.getColorOptional

object PotionFXLingering : ToggleableValueGroup(ModulePotionFX, "LingeringPotion", false) {

    private val extraRadius by float("ExtraRadius", 0f, 0f..10f)
    private val rotationSpeed by float("RotationSpeed", 1f, -10f..10f)
    private val canBeCovered by boolean("CanBeCovered", true)

    private val textureMode = modes(this, "Source", 0) {
        arrayOf(
            TextureMode.Builtin(it, PresetTexture.SIMPLE, enumSetAllOf<PresetTexture>()),
            TextureMode.Custom(it),
        )
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {

            val texture = textureMode.activeMode.texture ?: return@handler

            for (entity in world.entitiesForRendering()) {
                if (entity !is AreaEffectCloud) continue

                withPositionRelativeToCamera(entity.position().add(0.0, 0.01, 0.0))  {
                    poseStack.withPush {
                        val currentRotation = (entity.tickCount + event.partialTicks) * rotationSpeed

                        val color = when (val particle = entity.particle) {
                            is ColorParticleOption -> ARGB.color(
                                255,
                                (particle.red * 255).toInt(),
                                (particle.green * 255).toInt(),
                                (particle.blue * 255).toInt()
                            )
                            else -> Color4b.WHITE.argb
                        }

                        withPush {
                            mulPose(Axis.XP.rotationDegrees(-90f))
                            mulPose(Axis.ZP.rotationDegrees(currentRotation))
                            drawSquareTexture(
                                texture,
                                (entity.radius + extraRadius) * 2,
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
}
