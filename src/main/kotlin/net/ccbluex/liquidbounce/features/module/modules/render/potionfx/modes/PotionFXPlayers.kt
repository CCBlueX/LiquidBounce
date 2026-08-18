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
import net.ccbluex.liquidbounce.config.utils.TextureMode
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX
import net.ccbluex.liquidbounce.features.module.modules.render.potionfx.ModulePotionFX.PresetTexture
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.world.entityGetter
import net.ccbluex.liquidbounce.utils.world.filterTo
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.alchemy.PotionContents.getColorOptional

object PotionFXPlayers : ToggleableValueGroup(ModulePotionFX, "Players", false) {

    private val radius by float("Radius", 1f, 0.1f..10f)
    private val rotationSpeed by float("RotationSpeed", 4f, -10f..10f)
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

            for (entity in players) {
                withPositionRelativeToCamera(entity.getPosition(event.partialTicks).add(0.0, 0.01, 0.0)) {
                    poseStack.withPush {
                        val currentRotation = (entity.tickCount + event.partialTicks) * rotationSpeed

                        mulPose(Axis.XP.rotationDegrees(-90f))
                        mulPose(Axis.ZP.rotationDegrees(currentRotation))
                        drawSquareTexture(
                            texture,
                            radius * 2,
                            getColorOptional(entity.activeEffects).asInt,
                            AnchorPoint.CENTER,
                            noDepthTest = !canBeCovered
                        )
                    }
                }
            }
        }
    }

    val players by computedOn<GameTickEvent, MutableSet<Player>>(ReferenceOpenHashSet()) { _, set ->
        set.clear()
        if (!enabled) return@computedOn set
        world.entityGetter.filterTo(set, EntityTypes.PLAYER) {
            !it.activeEffects.isEmpty().and(!getColorOptional(it.activeEffects).isEmpty)
        }
        set
    }

    override fun onDisabled() {
        players.clear()
        super.onDisabled()
    }

}
