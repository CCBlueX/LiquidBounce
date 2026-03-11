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

package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor.UseHotbar
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.isPlayerArmor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

object AutoArmorSaveArmor : ToggleableValueGroup(ModuleAutoArmor, "SaveArmor", true) {
    val durabilityThreshold by int("DurabilityThreshold", 24, 0..100)
    private val autoOpen by boolean("AutoOpenInventory", true)

    private var hasOpenedInventory = false
    private var prevArmor = 0

    /**
     * Opens the inventory to save armor (as if the player has opened it manually) if the following conditions are met:
     * - The module is told to save armor and there is a replacement :)
     * - The inventory constraints require open inventory
     * (Otherwise, the inventory will be open automatically in a silent way and the armor will be saved)
     * - There is no replacement from the hotbar
     * (If there are some pieces that can be replaced by the pieces from the hotbar,
     * they will be used first, without opening the inventory)
     */
    @Suppress("unused")
    private val armorAutoSaveHandler = tickHandler {
        val conditions = booleanArrayOf(
            // Module checks
            ModuleAutoArmor.running,
            AutoArmorSaveArmor.enabled,

            // Game modes
            !player.isSpectator,
            !player.isCreative,

            // The module will automatically save armor if opening the inventory isn't required.
            ModuleAutoArmor.inventoryConstraints.requiresOpenInventory,
            autoOpen
        )

        // All conditions must be met for this feature to work.
        if (conditions.any { !it }) {
            return@tickHandler
        }

        /**
         * The server doesn't let the client know about the state of its armor items
         * when the player in a handled screen⁽¹⁾ like a chest, crafting table, anvil, etc.,
         * making the armor saving process not work at all when a handled screen is open.
         * In other words, `{player.inventory.armor}` doesn't get updated in such case.
         *
         * So, it's necessary to track the armor items and update their state (e.g. durability)
         * on the client side when the player receives damage, isn't it? :)
         * Well, unfortunately, it's not possible to do this accurately, not even close.
         * The server doesn't provide the client with enough data
         * to calculate the armor durability on the client side. :(
         *
         * Nonetheless, if the player is still in a handled screen⁽¹⁾
         * and gets an armor piece broken, his armor attribute, `{player.armor}`, gets updated.
         * This update lets the module know exactly when it should close the handled screen⁽¹⁾
         * so that the player equips a new armor piece.
         * Yes, this won't save armor pieces but might save the player's life.
         *
         * (1) - not including the player's own inventory which is also a handled screen.
         */
        val hasLostArmorPiece = shouldTrackArmor && player.armorValue < prevArmor
        prevArmor = player.armorValue

        // closes the current screen so that the armor slots are synced again
        if (hasLostArmorPiece) {
            player.closeContainer()
            return@tickHandler
        }

        val armorToEquipWithSlots = ArmorEvaluation
            .findBestArmorPieces(durabilityThreshold = durabilityThreshold)
            .values
            .filterNotNull()
            .filter { !it.isAlreadyEquipped && it.itemSlot.itemStack.isPlayerArmor }

        val hasAnyHotBarReplacement = booleanArrayOf(
            UseHotbar.enabled,
            UseHotbar.canSwapArmor,
            armorToEquipWithSlots.any { it.itemSlot is HotbarItemSlot }
        ).all { it }

        // the new pieces from the hotbar have a higher priority
        // due to the replacement speed (it's much faster, it makes sense to replace them first),
        // so it waits until all pieces from hotbar are replaced
        if (hasAnyHotBarReplacement) {
            return@tickHandler
        }

        val armorSlotsToEquip = armorToEquipWithSlots.mapTo(enumSetOf()) { ArmorPiece(it.itemSlot).slotType }

        val hasArmorToReplace = Slots.Armor.any {
            val armorStack = it.itemStack
            !armorStack.isEmpty && armorStack.durability <= durabilityThreshold
                && ArmorPiece(it).slotType in armorSlotsToEquip
        }

        // closes the inventory if the armor is replaced.
        closeInventory(hasArmorToEquip = armorSlotsToEquip.isNotEmpty())

        // tries to close the previous screen and open the inventory
        openInventory(hasArmorToReplace = hasArmorToReplace)
    }

    /**
     * Waits and closes the inventory after the armor is replaced.
     */
    private suspend fun closeInventory(hasArmorToEquip: Boolean) {
        if (!hasOpenedInventory || hasArmorToEquip) {
            return
        }

        this@AutoArmorSaveArmor.hasOpenedInventory = false
        waitTicks(ModuleAutoArmor.inventoryConstraints.closeDelay.random())

        // the current screen might change while the module is waiting
        if (mc.screen is InventoryScreen) {
            player.closeContainer()
        }
    }

    /**
     * Closes the previous game screen and opens the inventory.
     */
    private suspend fun openInventory(hasArmorToReplace: Boolean) {
        while (hasArmorToReplace && mc.screen !is InventoryScreen) {

            if (mc.screen is AbstractContainerScreen<*>) {
                // closes chests/crating tables/etc. (it never happens)
                player.closeContainer()
            } else if (mc.screen != null) {
                // closes ClickGUI, game chat, etc. to save some armor :)
                mc.screen!!.onClose()
            }

            waitTicks(1)

            // again, the current screen might change while the module is waiting
            if (mc.screen == null) {
                mc.setScreen(InventoryScreen(player))
                hasOpenedInventory = true
            }
        }
    }

    private val shouldTrackArmor : Boolean
        get() = mc.screen !is InventoryScreen && mc.screen is AbstractContainerScreen<*>
}
