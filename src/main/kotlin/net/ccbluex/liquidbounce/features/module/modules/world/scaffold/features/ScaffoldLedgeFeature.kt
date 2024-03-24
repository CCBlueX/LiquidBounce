/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

object ScaffoldLedgeFeature : ToggleableConfigurable(ModuleScaffold, "Ledge", false) {

    var sneakTime by int("SneakTime", 1, 0..10)

    var sneakTicks = 0

    fun ledge(simulatedPlayer: SimulatedPlayer, target: BlockPlacementTarget?, rotation: Rotation) {
        if (!enabled) {
            return
        }

        val ticks = ModuleScaffold.rotationsConfigurable.howLongItTakes(rotation)
        val simClone = simulatedPlayer.clone()
        simClone.tick()

        val ledgeSoon = simulatedPlayer.clipLedged || simClone.clipLedged

        if ((ticks >= 1 || !ModuleScaffold.hasBlockToBePlaced()) && ledgeSoon) {
            sneakTicks = sneakTime
        }

        // TODO: Decide what rotation to use, most accurate [rotation] or going with the [currentRotation]
        val currentCrosshairTarget = raycast(4.5,
            RotationManager.currentRotation ?: RotationManager.serverRotation)

        if ((target == null || currentCrosshairTarget == null)) {
            if (ledgeSoon) {
                sneakTicks = sneakTime
            }
        } else if (simulatedPlayer.clipLedged) {
            // Does the crosshair target meet the requirements?
            if (!target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget)
                || !ModuleScaffold.isValidCrosshairTarget(currentCrosshairTarget)) {
                sneakTicks = sneakTime
            }
        }


    }

    val handler = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        if (sneakTicks > 0) {
            it.sneaking = true
            sneakTicks--
        }
    }

}
