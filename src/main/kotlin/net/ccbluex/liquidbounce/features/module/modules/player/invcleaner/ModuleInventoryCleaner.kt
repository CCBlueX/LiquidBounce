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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoArmor
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */

object ModuleInventoryCleaner : Module("InventoryCleaner", Category.PLAYER) {
    private val inventoryConstraints = tree(InventoryConstraintsConfigurable())

    val maxBlocks by int("MaxBlocks", 512, 0..3000)
    val maxArrows by int("MaxArrows", 256, 0..3000)

    val usefulItems =
        items(
            "UsefulItems",
            mutableListOf(
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
                Items.ELYTRA,
            ),
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

    var hasClickedBefore = false

    val repeatable =
        repeatable {
            if (!canCurrentlyDoCleanup()) {
                return@repeatable
            }

            val cleanupPlan =
                CleanupPlanGenerator(cleanupTemplateFromSettings, findNonEmptySlotsInInventory()).generatePlan()

            for (hotbarSwap in cleanupPlan.swaps) {
                if (!canCurrentlyDoCleanup()) {
                    return@repeatable
                }

                // We can only swap to hotbar
                if (hotbarSwap.to !is HotbarItemSlot) {
                    continue
                }

                val fromServerId = hotbarSwap.from.getIdForServer(null) ?: continue

                val startDelay = inventoryConstraints.startDelay.random()

                val action =
                    tryRunActionInInventory(!hasClickedBefore && startDelay > 0) {
                        executeAction(fromServerId, hotbarSwap.to.hotbarSlotForServer, SlotActionType.SWAP)
                    }

                // This means the module has not clicked before, therefore apply start delay.
                if (action == null) {
                    hasClickedBefore = true

                    waitConditional(startDelay - 1) { !canCurrentlyDoCleanup() }

                    return@repeatable
                }

                if (action) {
                    hasClickedBefore = true

                    cleanupPlan.remapSlots(
                        hashMapOf(
                            Pair(hotbarSwap.from, hotbarSwap.to),
                            Pair(hotbarSwap.to, hotbarSwap.from),
                        ),
                    )

                    val delay = inventoryConstraints.clickDelay.random()

                    if (delay > 0) {
                        waitConditional(delay - 1) { !canCurrentlyDoCleanup() }

                        return@repeatable
                    }
                }
            }

            val stacksToMerge = ItemMerge.findStacksToMerge(cleanupPlan)

            for (slot in stacksToMerge) {
                if (!canCurrentlyDoCleanup()) {
                    return@repeatable
                }

                val serverSlotId = slot.getIdForServer(null) ?: continue

                val startDelay = inventoryConstraints.startDelay.random()

                val action =
                    tryRunActionInInventory(!hasClickedBefore && startDelay > 0) {
                        executeAction(serverSlotId, 0, SlotActionType.PICKUP)
                        executeAction(serverSlotId, 0, SlotActionType.PICKUP_ALL)
                        executeAction(serverSlotId, 0, SlotActionType.PICKUP)
                    }

                // This means the module has not clicked before, therefore apply start delay.
                if (action == null) {
                    hasClickedBefore = true

                    waitConditional(startDelay - 1) { !canCurrentlyDoCleanup() }

                    return@repeatable
                }

                if (action) {
                    hasClickedBefore = true

                    val delay = inventoryConstraints.clickDelay.random()

                    if (delay > 0) {
                        waitConditional(delay - 1) { !canCurrentlyDoCleanup() }

                        return@repeatable
                    }
                }
            }

            // It is important that we call findItemSlotsInInventory() here again, because the inventory has changed.
            val itemsToThrowOut = findItemsToThrowOut(cleanupPlan, findNonEmptySlotsInInventory())

            for (slot in itemsToThrowOut) {
                if (!canCurrentlyDoCleanup()) {
                    return@repeatable
                }

                val serverSlotId = slot.getIdForServer(null) ?: continue

                val startDelay = inventoryConstraints.startDelay.random()

                val action =
                    tryRunActionInInventory(!hasClickedBefore && startDelay > 0) {
                        executeAction(serverSlotId, 1, SlotActionType.THROW)
                    }

                // This means the module has not clicked before, therefore apply start delay.
                if (action == null) {
                    hasClickedBefore = true

                    waitConditional(startDelay - 1) { !canCurrentlyDoCleanup() }

                    return@repeatable
                }

                if (action) {
                    hasClickedBefore = true

                    val delay = inventoryConstraints.clickDelay.random()

                    if (delay > 0) {
                        waitConditional(delay - 1) { !canCurrentlyDoCleanup() }

                        return@repeatable
                    }
                }
            }

            if (hasClickedBefore && canCloseMainInventory) {
                waitConditional(inventoryConstraints.closeDelay.random()) { !canCurrentlyDoCleanup() }

                // Can we still close the inventory or has something changed?
                if (canCloseMainInventory) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }

                hasClickedBefore = false
            }
        }

    val cleanupTemplateFromSettings: CleanupPlanPlacementTemplate
        get() =
            CleanupPlanPlacementTemplate(
                hashMapOf(
                    Pair(OffHandSlot, offHandItem),
                    Pair(HotbarItemSlot(0), slotItem1),
                    Pair(HotbarItemSlot(1), slotItem2),
                    Pair(HotbarItemSlot(2), slotItem3),
                    Pair(HotbarItemSlot(3), slotItem4),
                    Pair(HotbarItemSlot(4), slotItem5),
                    Pair(HotbarItemSlot(5), slotItem6),
                    Pair(HotbarItemSlot(6), slotItem7),
                    Pair(HotbarItemSlot(7), slotItem8),
                    Pair(HotbarItemSlot(8), slotItem9),
                ),
                itemLimitPerCategory =
                    hashMapOf(
                        Pair(ItemSortChoice.BLOCK.category!!, maxBlocks),
                        Pair(ItemCategory(ItemType.ARROW, 0), maxArrows),
                    ),
                isGreedy = isGreedy,
            )

    fun findItemsToThrowOut(
        cleanupPlan: InventoryCleanupPlan,
        itemsInInv: List<ItemSlot>,
    ): List<ItemSlot> {
        return itemsInInv.filter { it !in cleanupPlan.usefulItems }
    }

    private fun tryRunActionInInventory(
        delayFirstClick: Boolean,
        action: () -> Unit,
    ): Boolean? {
        if (canCurrentlyDoCleanup()) {
            openInventorySilently()

            // Is this the first time? Return abnormal result if so
            if (delayFirstClick) {
                return null
            }

            action()

            return true
        }

        return false
    }

    private fun executeAction(
        slotIdForServer: Int,
        clickData: Int,
        slotActionType: SlotActionType,
    ) {
        interaction.clickSlot(0, slotIdForServer, clickData, slotActionType, player)
    }

    private fun canCurrentlyDoCleanup(): Boolean {
        val old = hasClickedBefore

        hasClickedBefore = false

        if (player.currentScreenHandler.syncId != 0 || interaction.hasRidingInventory()) {
            return false
        }

        if (ModuleAutoArmor.locked || isNoMoveViolated()) {
            return false
        }

        if (inventoryConstraints.invOpen && !isInInventoryScreen) {
            return false
        }

        hasClickedBefore = old

        return true
    }

    private fun isNoMoveViolated(): Boolean {
        if (inventoryConstraints.violatesNoMove) {
            if (canCloseMainInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            return true
        }

        return false
    }

    override fun disable() {
        if (canCloseMainInventory) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        hasClickedBefore = false
    }
}
