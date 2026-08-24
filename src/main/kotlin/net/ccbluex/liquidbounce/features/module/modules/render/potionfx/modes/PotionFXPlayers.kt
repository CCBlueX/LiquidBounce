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
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.PresetTexture
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.SecondaryPresetTexture
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXPlayers.MainEffect.radius
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXPlayers.MainEffect.rotationSpeed
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXPlayers.MainEffect.textureMode
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXPlayers.SecondEffect.extraRadius
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.modes.PotionFXPlayers.SecondEffect.secondaryTextureMode
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.world.EntityLookup.Companion.EntityLookup
import net.ccbluex.liquidbounce.utils.world.filterTo
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.alchemy.PotionContents.getColorOptional

object PotionFXPlayers : ToggleableValueGroup(ModulePotionFX, "Players", true) {

    private object MainEffect : ValueGroup("MainEffect") {
        val radius by float("Radius", 1f, 0.1f..10f)
        val rotationSpeed by float("RotationSpeed", 4f, -10f..10f)

        val textureMode = modes(this@PotionFXPlayers, "Source", 0) {
            arrayOf(
                TextureMode.Builtin(it, PresetTexture.DASHED, enumSetAllOf<PresetTexture>()),
                TextureMode.Custom(it),
            )
        }
    }

    private object SecondEffect : ToggleableValueGroup(this, "SecondEffect", false) {
        val rotationSpeed by float("RotationSpeed", 4f, -10f..10f)
        val extraRadius by float("ExtraRadius", 0f, 0f..10f)
        val secondaryTextureMode = modes(this, "Source", 0) {
            arrayOf(
                TextureMode.Builtin(it, SecondaryPresetTexture.CRACKED, enumSetAllOf<SecondaryPresetTexture>()),
                TextureMode.Custom(it),
            )
        }
    }

    init {
        tree(MainEffect)
        tree(SecondEffect)
    }

    private val canBeCovered by boolean("CanBeCovered", true)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            val texture = textureMode.activeMode.texture ?: return@handler
            val secondaryTexture = secondaryTextureMode.activeMode.texture ?: return@handler
            if (players.isEmpty()) return@handler

            for (entity in players) {
                withPositionRelativeToCamera(entity.getPosition(event.partialTicks).add(0.0, 0.01, 0.0)) {
                    poseStack.withPush {
                        val rotation = (entity.tickCount + event.partialTicks) * rotationSpeed
                        val secondRotation = (entity.tickCount + event.partialTicks) * SecondEffect.rotationSpeed

                        withPush {
                            mulPose(Axis.XP.rotationDegrees(-90f))
                            mulPose(Axis.ZP.rotationDegrees(rotation))
                            drawSquareTexture(
                                texture,
                                radius * 2,
                                getColorOptional(entity.activeEffects).asInt,
                                AnchorPoint.CENTER,
                                noDepthTest = !canBeCovered
                            )
                        }
                        if (SecondEffect.enabled) {
                            withPush {
                                mulPose(Axis.XP.rotationDegrees(-90f))
                                mulPose(Axis.ZP.rotationDegrees(secondRotation))
                                drawSquareTexture(
                                    secondaryTexture,
                                    (radius + extraRadius) * 2,
                                    getColorOptional(entity.activeEffects).asInt,
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

    private val players by EntityLookup { set ->
        filterTo(
            set,
            EntityTypes.PLAYER
        ) { !it.activeEffects.isEmpty() && (!getColorOptional(it.activeEffects).isEmpty) }
    }

    override fun onDisabled() {
        players.clear()
        super.onDisabled()
    }

}
