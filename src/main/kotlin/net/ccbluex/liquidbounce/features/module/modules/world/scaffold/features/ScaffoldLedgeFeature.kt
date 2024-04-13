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
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import kotlin.math.max

data class LedgeState(
    val requiresJump: Boolean,
    val requiresSneak: Int
) {
    companion object {
        val NO_LEDGE = LedgeState(requiresJump = false, requiresSneak = 0)
    }
}

fun ledge(
    directionalInput: DirectionalInput,
    simulatedPlayer: SimulatedPlayer,
    target: BlockPlacementTarget?,
    rotation: Rotation,
    extension: ScaffoldLedgeExtension? = null
): LedgeState {
    val ticks = ModuleScaffold.ScaffoldRotationConfigurable.howLongToReach(rotation)
    val simClone = simulatedPlayer.clone()
    simClone.tick()

    // [ledgeSoon] could be replaced with isCloseToEdge, but I feel like this is more consistent
    val ledgeSoon = simulatedPlayer.clipLedged || simClone.clipLedged

    if ((ticks >= 1 || !ModuleScaffold.hasBlockToBePlaced()) && ledgeSoon) {
        return LedgeState(requiresJump = false, requiresSneak = max(1, ticks))
    }

    return extension?.ledge(
        ledge = simulatedPlayer.clipLedged,
        ledgeSoon = ledgeSoon,
        target = target,
        rotation = rotation
    ) ?: LedgeState.NO_LEDGE
}

interface ScaffoldLedgeExtension {
    fun ledge(
        ledge: Boolean,
        ledgeSoon: Boolean,
        target: BlockPlacementTarget?,
        rotation: Rotation
    ): LedgeState
}
