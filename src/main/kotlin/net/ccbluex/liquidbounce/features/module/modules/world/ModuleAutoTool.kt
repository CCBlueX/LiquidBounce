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
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
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
     * Useful if the tool have special effects
     * cannot be determined
     *
     * NOTE: option [search] must be disabled
     */
    private val slot by int("Slot", 0, 0..8)

    // Swap the slot of the previous item
    private val swapPrevious by boolean("SwapPrevious", false)

    // Time (in ticks) to swap to the previous slot (0.5s - 2.5s)
    private val time by int("Time", 25, 10..50)

    // Time left to swap
    private var leftTime = 0

    // Previous slot
    private var prev = 0

    val handler = handler<BlockBreakingProgressEvent> { event ->
        val blockState = world.getBlockState(event.pos)
        val inventory = player.inventory
        if (swapPrevious) {
            leftTime = time
        }
        val index = if (search) {
            val (hotbarSlot, _) = (0..8)
                .map { Pair(it, inventory.getStack(it)) }
                .filter {
                    val stack = it.second
                    (stack.isNothing() || (!player.isCreative && (stack.damage < (stack.maxDamage - 2) || ignoreDurability)))
                }
                .maxByOrNull {
                    val stack = it.second
                    stack.getMiningSpeedMultiplier(blockState)
                } ?: return@handler
            hotbarSlot
        } else {
            slot
        }
        if (inventory.selectedSlot == index) {
            return@handler
        }
        prev = inventory.selectedSlot
        inventory.selectedSlot = index
        inventory.updateItems()
    }

    val playerTickHandler = handler<PlayerTickEvent> {
        if (prev == -1) {
            return@handler
        }
        if (leftTime > 0) {
            if (leftTime == 1) {
                player.inventory.selectedSlot = prev
                prev = -1
            }
            leftTime--
        }
    }
}
