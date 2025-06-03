/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import kotlin.math.max

data class LedgeAction(
    val jump: Boolean = false,
    val sneakTime: Int = 0,
    val stopInput: Boolean = false,
    val stepBack: Boolean = false
) {
    companion object {
        val NO_LEDGE = LedgeAction(jump = false, sneakTime = 0, stopInput = false)
    }

}

fun ledge(
    target: BlockPlacementTarget?,
    rotation: Rotation,
    extension: ScaffoldLedgeExtension? = null
): LedgeAction {
    if (player.isCloseToEdge(DirectionalInput(player.input))) {
        val ticks = ModuleScaffold.ScaffoldRotationConfigurable.calculateTicks(rotation)

        ModuleDebug.debugParameter(ModuleScaffold, "TicksUntilDestination", ticks)

        val isLowOnBlocks = ModuleScaffold.blockCount <= 0
        val isNotReady = ticks >= 1

        if (isLowOnBlocks || isNotReady) {
            return LedgeAction(jump = false, sneakTime = max(1, ticks))
        }
    }

    return extension?.ledge(
        target = target,
        rotation = rotation
    ) ?: LedgeAction.NO_LEDGE
}

interface ScaffoldLedgeExtension {
    fun ledge(
        target: BlockPlacementTarget?,
        rotation: Rotation
    ): LedgeAction
}
