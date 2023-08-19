/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.item.isNothing

/**
 * AutoTool module
 *
 * Automatically chooses the best tool in your inventory to mine a block.
 */

object ModuleAutoTool : Module("AutoTool", Category.WORLD) {

    // Ignore items with low durability
    private val ignoreDurability by boolean("IgnoreDurability", false)

    // Automatic search for the best weapon
    private val search by boolean("Search", true)

    /* Slot with the best tool
     * Useful if the tool has special effects
     * cannot be determined
     *
     * NOTE: option [search] must be disabled
     */
    private val slot by int("Slot", 0, 0..8)

    private val swapPreviousDelay by int("SwapPreviousDelay", 20, 1..100)

    val handler = handler<BlockBreakingProgressEvent> { event ->
        val blockState = world.getBlockState(event.pos)
        val inventory = player.inventory
        val index = if (search) {
            val (hotbarSlot, stack) = (0..8).map { Pair(it, inventory.getStack(it)) }.filter {
                val stack = it.second
                (stack.isNothing() || (!player.isCreative && (stack.damage < (stack.maxDamage - 2) || ignoreDurability)))
            }.maxByOrNull {
                val stack = it.second
                stack.getMiningSpeedMultiplier(blockState)
            } ?: return@handler
            if (stack.getMiningSpeedMultiplier(blockState) == player.inventory.mainHandStack.getMiningSpeedMultiplier(
                    blockState
                )
            ) return@handler
            // no point in switching slots

            hotbarSlot
        } else {
            slot
        }
        SilentHotbar.selectSlotSilently(this, index, swapPreviousDelay)
    }
}
