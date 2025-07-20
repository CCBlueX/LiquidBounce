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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.math.random.Random

/**
 * Vomit module
 *
 * Drops items from the inventory in a random order to make it look like the player is vomiting.
 * If the player is in creative mode, the player will drop random block items.
 */
object ModuleVomit : ClientModule("Vomit", Category.FUN) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())
    private val random = Random.create()

    @Suppress("unused")
    private val vomitHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (player.isCreative) {
            val blockItem = Registries.BLOCK.getRandom(random).get().value()
            val randomStack = ItemStack(blockItem, 64)
            val emptySlots = findEmptyStorageSlotsInInventory()

            if (emptySlots.isEmpty()) {
                // Throw only - this will rate limit after a few stacks
                event.schedule(inventoryConstraints, CreativeInventoryAction.performThrow(randomStack))
                return@handler
            }

            // Fill and throw - this bypasses the creative drop limit
            event.schedule(inventoryConstraints, if (inventoryConstraints.clickDelay.last <= 0) {
                // Depending on how many empty slots we have, this might kick in the packet rate limit
                // of ViaVersion or Minecraft/Paper itself
                emptySlots.map { slot -> CreativeInventoryAction.performFillSlot(randomStack, slot) } +
                    emptySlots.map { slot -> ClickInventoryAction.performThrow(null, slot) }
            } else {
                val slot = emptySlots.random()

                listOf(
                    CreativeInventoryAction.performFillSlot(randomStack, slot),
                    ClickInventoryAction.performThrow(null, slot)
                )
            })
        } else {
            // We specifically only want to choose slots that we can store items in, as
            // e.g. the offhand slot is not a storage slot on 1.8 servers and therefore can cause issues
            val playerSlot = findNonEmptyStorageSlotsInInventory()
            val container = mc.currentScreen as? GenericContainerScreen

            val randomSlot = if (playerSlot.isEmpty()) {
                // Attempt to drop from the container
                val slots = findItemsInContainer(container ?: return@handler)
                if (slots.isEmpty()) return@handler

                slots.random()
            } else {
                playerSlot.random()
            }

            event.schedule(inventoryConstraints, ClickInventoryAction.performThrow(container, randomSlot))
        }
    }


}
