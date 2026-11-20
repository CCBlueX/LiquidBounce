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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.findEmptyStorageSlotsInInventory
import net.ccbluex.liquidbounce.utils.inventory.findItemsInContainer
import net.ccbluex.liquidbounce.utils.inventory.findNonEmptyStorageSlotsInInventory
import net.ccbluex.liquidbounce.utils.inventory.inventoryPlan
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack

/**
 * Vomit module
 *
 * Drops items from the inventory in a random order to make it look like the player is vomiting.
 * If the player is in creative mode, the player will drop random block items.
 */
object ModuleVomit : ClientModule("Vomit", ModuleCategories.FUN) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())
    private val random = RandomSource.create()

    @Suppress("unused")
    private val vomit = inventoryPlan(inventoryConstraints) {
        val isCreative = player.isCreative
        requires { player.isCreative == isCreative }

        if (isCreative) {
            val blockItem = BuiltInRegistries.BLOCK.getRandom(random).get().value()
            val randomStack = ItemStack(blockItem, 64)
            val emptySlots = findEmptyStorageSlotsInInventory()

            if (emptySlots.isEmpty()) {
                // Throw only - this will rate limit after a few stacks
                perform(InventoryAction.Creative.performThrow(randomStack))
                return@inventoryPlan
            }

            // Fill and throw - this bypasses the creative drop limit
            perform(if (inventoryConstraints.clickDelay.last <= 0) {
                // Depending on how many empty slots we have, this might kick in the packet rate limit
                // of ViaVersion or Minecraft/Paper itself
                emptySlots.map { slot -> InventoryAction.Creative.performFillSlot(randomStack, slot) } +
                    emptySlots.map { slot -> InventoryAction.Click.performThrow(null, slot) }
            } else {
                val slot = emptySlots.random()

                listOf(
                    InventoryAction.Creative.performFillSlot(randomStack, slot),
                    InventoryAction.Click.performThrow(null, slot)
                )
            })
        } else {
            // We specifically only want to choose slots that we can store items in, as
            // e.g. the offhand slot is not a storage slot on 1.8 servers and therefore can cause issues
            val playerSlot = findNonEmptyStorageSlotsInInventory()
            val container = mc.gui.screen() as? ContainerScreen

            val randomSlot = if (playerSlot.isEmpty()) {
                // Attempt to drop from the container
                val slots = (container ?: return@inventoryPlan).findItemsInContainer()
                if (slots.isEmpty()) return@inventoryPlan

                slots.random()
            } else {
                playerSlot.random()
            }

            perform(InventoryAction.Click.performThrow(container, randomSlot))
        }
    }


}
