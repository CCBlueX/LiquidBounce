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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.InventoryItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand

/**
 * AutoSoup module
 *
 * Automatically eats soup whenever your health is low.
 */

object ModuleAutoSoup : Module("AutoSoup", Category.COMBAT) {

    private val health by int("Health", 15, 1..40)

    object DropAfterUse : ToggleableConfigurable(this, "DropAfterUse", true) {
        val assumeEmptyBowl by boolean("AssumeEmptyBowl", true)
        val itemDropDelay by intRange("ItemDropDelay", 1..2, 1..40, "ticks")
    }

    object Refill : ToggleableConfigurable(this, "Refill", true) {

        private val inventoryConstraints = tree(InventoryConstraintsConfigurable())

        val repeat = repeatable {
            // Check if there is space in the hotbar for a soup
            if (!findEmptyHotbarSlot()) {
                return@repeatable
            }

            // Check if there is a soup in the inventory
            val soupSlot = ALL_SLOTS_IN_INVENTORY.find { it is InventoryItemSlot &&
                it.itemStack.item == Items.MUSHROOM_STEW } ?: return@repeatable

            performInventoryClick(soupSlot)
        }

        private fun findEmptyHotbarSlot(): Boolean {
            return ALL_SLOTS_IN_INVENTORY.find {
                it.slotType == ItemSlotType.HOTBAR && it.itemStack.isNothing()
            } != null
        }

        private fun shouldCancelInvMove(): Boolean {
            if (inventoryConstraints.violatesNoMove) {
                if (canCloseMainInventory) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }

                return true
            }

            if (inventoryConstraints.invOpen && !isInInventoryScreen) {
                return true
            }

            if (!player.currentScreenHandler.isPlayerInventory) {
                return true
            }

            return false
        }

        private suspend fun Sequence<DummyEvent>.performInventoryClick(item: ItemSlot): Boolean {
            if (shouldCancelInvMove()) {
                return false
            }

            val slot = item.getIdForServerWithCurrentScreen() ?: return false

            if (!isInInventoryScreen) {
                openInventorySilently()
            }

            val startDelay = inventoryConstraints.startDelay.random()

            if (startDelay > 0) {
                if (!waitConditional(startDelay) { shouldCancelInvMove() }) {
                    return false
                }
            }

            interaction.clickSlot(0, slot, 0, SlotActionType.QUICK_MOVE, player)

            if (canCloseMainInventory) {
                waitConditional(inventoryConstraints.closeDelay.random()) { shouldCancelInvMove() }

                // Can it still be closed?
                if (canCloseMainInventory) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }
            }

            return true
        }

    }

    init {
        tree(DropAfterUse)
        tree(Refill)
    }

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val swapPreviousDelay by int("SwapPreviousAdditionalDelay", 5, 1..100, "ticks")

    val repeatable = repeatable {
        val mushroomStewSlot = findHotbarSlot(Items.MUSHROOM_STEW)

        if (interaction.hasRidingInventory()) {
            return@repeatable
        }

        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        if (player.health < health && mushroomStewSlot != null && !isInInventoryScreen) {
            // We need to take some actions
            CombatManager.pauseCombatForAtLeast(combatPauseTime)

            if (player.isBlocking) {
                interaction.stopUsingItem(player)
                waitTicks(1)
            }

            // Calculate the delay until the item is dropped
            val itemDropDelay = if (DropAfterUse.enabled) DropAfterUse.itemDropDelay.random() else 0

            if (mushroomStewSlot != player.inventory.selectedSlot) {
                SilentHotbar.selectSlotSilently(this, mushroomStewSlot,
                    swapPreviousDelay + itemDropDelay)
            }

            // Use soup
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence)
            }

            // If action was successful, drop the now-empty bowl
            if (DropAfterUse.enabled) {
                waitTicks(itemDropDelay)

                if (DropAfterUse.assumeEmptyBowl || player.inventory.getStack(mushroomStewSlot).item == Items.BOWL) {
                    player.dropSelectedItem(true)
                }
            }
            return@repeatable
        }
    }
}

