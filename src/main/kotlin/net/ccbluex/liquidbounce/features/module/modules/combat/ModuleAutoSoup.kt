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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand

/**
 * AutoSoup module
 *
 * Automatically eats soup whenever your health is low.
 */

object ModuleAutoSoup : Module("AutoSoup", Category.COMBAT) {

    private val health by int("Health", 15, 1..40)
    private val bowl by boolean("DropAfterUse", true)
    private val combatPauseTime by int("CombatPauseTime", 0, 0..40)
    private val itemDropDelay by intRange("ItemDropDelay", 1..2, 0..40)
    private val swapPreviousDelay by int("SwapPreviousAdditionalDelay", 5, 1..100)

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
                wait(1)
            }

            val itemDrop = itemDropDelay.random()

            if (mushroomStewSlot != player.inventory.selectedSlot) {
                SilentHotbar.selectSlotSilently(this, mushroomStewSlot, swapPreviousDelay + itemDrop)
            }

            // Use soup
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence)
            }

            // If action was successful, drop the now-empty bowl
            if (bowl && player.inventory.getStack(mushroomStewSlot).item == Items.BOWL) {
                wait(itemDrop)
                player.dropSelectedItem(true)
            }
            return@repeatable
        }
    }
}
