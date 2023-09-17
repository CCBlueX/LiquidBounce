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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.*
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.fluid.LavaFluid
import net.minecraft.fluid.WaterFluid
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType
import kotlin.collections.HashMap
import kotlin.math.ceil

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */

object ModuleInventoryCleaner : Module("InventoryCleaner", Category.PLAYER) {

    val inventoryConstraints = tree(InventoryConstraintsConfigurable())

    val maxBlocks by int("MaxBlocks", 512, 0..3000)
    val maxArrows by int("MaxArrows", 256, 0..3000)

    val usefulItems = items(
        "UsefulItems", mutableListOf(
            Items.WATER_BUCKET,
            Items.LAVA_BUCKET,
            Items.MILK_BUCKET,
            Items.FLINT_AND_STEEL,
            Items.ENDER_PEARL,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.ARROW,
            Items.SPECTRAL_ARROW,
            Items.TIPPED_ARROW,
            Items.POTION,
            Items.LINGERING_POTION,
            Items.SPLASH_POTION,
            Items.TRIDENT,
            Items.TNT,
            Items.ELYTRA
        )
    )

    val isGreedy by boolean("Greedy", true)

    val offHandItem by enumChoice("OffHandItem", ItemSortChoice.SHIELD, ItemSortChoice.values())
    val slotItem1 by enumChoice("SlotItem-1", ItemSortChoice.SWORD, ItemSortChoice.values())
    val slotItem2 by enumChoice("SlotItem-2", ItemSortChoice.BOW, ItemSortChoice.values())
    val slotItem3 by enumChoice("SlotItem-3", ItemSortChoice.PICKAXE, ItemSortChoice.values())
    val slotItem4 by enumChoice("SlotItem-4", ItemSortChoice.AXE, ItemSortChoice.values())
    val slotItem5 by enumChoice("SlotItem-5", ItemSortChoice.NONE, ItemSortChoice.values())
    val slotItem6 by enumChoice("SlotItem-6", ItemSortChoice.NONE, ItemSortChoice.values())
    val slotItem7 by enumChoice("SlotItem-7", ItemSortChoice.FOOD, ItemSortChoice.values())
    val slotItem8 by enumChoice("SlotItem-8", ItemSortChoice.BLOCK, ItemSortChoice.values())
    val slotItem9 by enumChoice("SlotItem-9", ItemSortChoice.BLOCK, ItemSortChoice.values())

    val repeatable = repeatable {
        if (player.currentScreenHandler.syncId != 0 || interaction.hasRidingInventory()) {
            return@repeatable
        }

        if (ModuleAutoArmor.locked) {
            return@repeatable
        }

        val cleanupPlan = createCleanupPlan()

        for (hotbarSwap in cleanupPlan.hotbarSwaps) {
            if (tryRunActionInInventory { executeAction(hotbarSwap.from, hotbarSwap.to, SlotActionType.SWAP) }) {
                wait { inventoryConstraints.delay.random() }

                return@repeatable
            }
        }

        val stacksToMerge = ItemMerge.findStacksToMerge(cleanupPlan)

        for (i in stacksToMerge) {
            val shouldReturn = tryRunActionInInventory {
                executeAction(i, 0, SlotActionType.PICKUP)
                executeAction(i, 0, SlotActionType.PICKUP_ALL)
                executeAction(i, 0, SlotActionType.PICKUP)
            }

            if (shouldReturn) {
                wait { inventoryConstraints.delay.random() }

                return@repeatable
            }
        }

        val itemsToThrowOut = findItemsToThrowOut(cleanupPlan)

        for (i in itemsToThrowOut) {
            if (tryRunActionInInventory { executeAction(i, 1, SlotActionType.THROW) }) {
                wait { inventoryConstraints.delay.random() }

                return@repeatable
            }
        }
    }

    fun findItemsToThrowOut(cleanupPlan: InventoryCleanupPlan): List<Int> {
        val itemsToThrowOut = mutableListOf<Int>()

        for (i in 0..40) {
            if (player.inventory.getStack(i).isNothing() || i in cleanupPlan.usefulItems) {
                continue
            }

            itemsToThrowOut.add(i)
        }

        return itemsToThrowOut
    }

    private fun tryRunActionInInventory(action: () -> Unit): Boolean {
        val isInInventoryScreen = mc.currentScreen is InventoryScreen

        if (!isInInventoryScreen) {
            openInventorySilently()
        }

        if (!(inventoryConstraints.noMove && player.moving) && (!inventoryConstraints.invOpen || isInInventoryScreen)) {
            action()

            if (!isInInventoryScreen) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            return true
        }

        return false
    }

    private fun executeAction(slot: Int, clickData: Int, slotActionType: SlotActionType) {
        val remappedSlot = convertClientSlotToServerSlot(slot)

        interaction.clickSlot(0, remappedSlot, clickData, slotActionType, player)
    }
}
