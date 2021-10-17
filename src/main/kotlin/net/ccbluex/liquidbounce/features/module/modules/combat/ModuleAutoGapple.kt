/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.utils.item.convertClientSlotToServerSlot
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.util.InputUtil
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType

/**
 * AutoGapple module
 *
 * Automatically eats apples whenever your health is low.
 */

object ModuleAutoGapple : Module("AutoGapple", Category.COMBAT) {

    val health by int("Health", 18, 1..20)

    var prevSlot = -1
    var eating = false
    var saveSlot = false

    override fun disable() {
        if (!InputUtil.isKeyPressed(mc.window.handle, mc.options.keyUse.boundKey.code)) {
            mc.options.keyUse.isPressed = false
        }
    }

    val repeatable = repeatable {
        // Check first with Hotbar and see if it has any apples
        val slot = findHotbarSlot(Items.GOLDEN_APPLE)

        val invSlot = findInventorySlot(Items.GOLDEN_APPLE)

        // If both have been checked but neither of these provide any result
        if (slot == null && invSlot == null) {
            if (eating) {
                player.inventory.selectedSlot = prevSlot
            } else {
                return@repeatable
            }
        }

        if (player.isDead) {
            return@repeatable
        }

        if (player.health < health) {
            if (slot != null) {
                if (!saveSlot) {
                    prevSlot = player.inventory.selectedSlot
                    saveSlot = true
                }
                player.inventory.selectedSlot = slot

                // Avoid sword shield
                wait(2)
                eating = true
                mc.options.keyUse.isPressed = true
            } else {
                // If there's no apples in the hotbar slot though, start checking on inventory
                val serverSlot = convertClientSlotToServerSlot(invSlot!!)

                val openInventory = mc.currentScreen !is InventoryScreen

                if (openInventory) {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY))
                }

                interaction.clickSlot(0, serverSlot, 0, SlotActionType.QUICK_MOVE, player)

                if (openInventory) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }
                return@repeatable
            }
        }

        if (eating && player.health + player.absorptionAmount >= health) {
            saveSlot = false
            eating = false
            mc.options.keyUse.isPressed = false
            player.inventory.selectedSlot = prevSlot
        }
    }
}
