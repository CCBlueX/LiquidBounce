/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * AutoTool module
 *
 * Automatically chooses the best tool in your inventory to mine a block.
 */

object ModuleAutoTool : Module("AutoTool", Category.WORLD) {

    val handler = handler<BlockBreakingProgressEvent> { event ->
        val blockState = world.getBlockState(event.pos)
        var bestSlot: Int? = null
        var bestSpeed = 1f

        for (i in 0..8) {
            val item = player.inventory.getStack(i)
            val speed = item.getMiningSpeedMultiplier(blockState)

            if (speed > bestSpeed) {
                bestSpeed = speed
                bestSlot = i
            }
        }

        if (bestSlot != null) {
            player.inventory.selectedSlot = bestSlot
        }
    }
}
