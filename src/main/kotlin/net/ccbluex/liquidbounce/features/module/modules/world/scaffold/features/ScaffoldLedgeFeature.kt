/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.Rotation
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

        val ticks = ModuleScaffold.rotationsConfigurable.howLongToReach(rotation)
        val simClone = simulatedPlayer.clone()
        simClone.tick()

        val ledgeSoon = simulatedPlayer.clipLedged || simClone.clipLedged

        if ((ticks >= 1 || !ModuleScaffold.hasBlockToBePlaced()) && ledgeSoon) {
            sneakTicks = sneakTime
        }

        // todo: introduce rotation prediction because currently I abuse [howLongItTakes] to get the ticks
        //   and simply check for the correct rotation without considering the Rotation Manager at all
        val currentCrosshairTarget = raycast(4.5, rotation)

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

    @Suppress("unused")
    private val handler = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        if (sneakTicks > 0) {
            it.sneaking = true
            sneakTicks--
        }
    }

}
