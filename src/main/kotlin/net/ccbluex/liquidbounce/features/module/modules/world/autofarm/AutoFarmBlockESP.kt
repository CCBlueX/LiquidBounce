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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawBoxSides
import net.ccbluex.liquidbounce.render.effect.ShaderLayer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.sq

/**
 * Highlights crops ready for harvest, blocks to plant on and the block being farmed.
 */
object AutoFarmBlockESP : ToggleableValueGroup(ModuleAutoFarm, "BlockESP", true) {

    private val readyColor by color("ReadyColor", Color4b(36, 237, 0, 255))
    private val placeColor by color("PlaceColor", Color4b(191, 245, 66))
    private val range by int("Range", 50, 10..128)

    private val colorRainbow by boolean("Rainbow", false)

    private val placeTargets by multiEnumChoice("PlaceTargets", AutoFarmTrackedState.Plantable.entries)

    private val shader = tree(ShaderLayer())

    private object CurrentTarget : ToggleableValueGroup(AutoFarmBlockESP, "CurrentTarget", true) {
        private val color by color("Color", Color4b(66, 120, 245, 255))
        private val colorRainbow by boolean("Rainbow", false)

        fun render(renderEnvironment: WorldRenderEnvironment) {
            if (!this.enabled) return
            val target = ModuleAutoFarm.currentTarget ?: return
            with(renderEnvironment) {
                withPositionRelativeToCamera(target) {
                    drawBox(FULL_BOX, if (colorRainbow) rainbow() else color)
                }
            }
        }
    }

    init {
        tree(CurrentTarget)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val harvestColor = if (colorRainbow) rainbow() else readyColor

        event.renderEnvironment(shader) {
            CurrentTarget.render(this)
            for ((pos, type) in AutoFarmBlockTracker.iterate()) {
                if (player.position().horizontalDistanceToSqr(pos) > range.sq()) continue

                withPositionRelativeToCamera(pos) {
                    when (type) {
                        AutoFarmTrackedState.ReadyForHarvest -> drawBox(FULL_BOX, harvestColor)

                        is AutoFarmTrackedState.Plantable -> if (type in placeTargets) {
                            val availableSides = type.findPlantableSides(pos, world.getBlockState(pos))
                            drawBoxSides(FULL_BOX, sides = availableSides, faceColor = placeColor)
                        }

                        AutoFarmTrackedState.Bonemealable -> {
                            // NOOP
                        }
                    }
                }
            }
        }
    }

}
