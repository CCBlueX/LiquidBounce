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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.item.convertClientSlotToServerSlot
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * AutoSoup module
 *
 * Automatically eats soup whenever your health is low.
 */

object ModuleAutoSoup : Module("AutoSoup", Category.COMBAT) {

    private val bowl by enumChoice("Bowl", BowlMode.DROP, BowlMode.values())
    private val health by int("Health", 18, 1..20)

    private var lastSlot = -1
    private var saveSlot = false

    val repeatable = repeatable {
        val hotBarSlot = (0..8).firstOrNull {
            hasItem(it, Items.MUSHROOM_STEW)
        }

        val bowlSlot = (0..8).firstOrNull {
            hasItem(it, Items.BOWL)
        }

        val invSlot = (9..35).find {
            hasItem(it, Items.MUSHROOM_STEW)
        }

        if (hotBarSlot == null && invSlot == null && bowlSlot == null) {
            return@repeatable
        }

        if (player.isDead) {
            return@repeatable
        }

        if (bowlSlot != null) {
            network.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    Direction.DOWN
                )
            )
            if (saveSlot) {
                player.inventory.selectedSlot = lastSlot
                saveSlot = false
            }
            when (bowl) {
                BowlMode.DROP -> {
                    utilizeInventory(bowlSlot, 1, SlotActionType.THROW, false)
                }
                BowlMode.MOVE -> {
                    val openInventory = mc.currentScreen !is InventoryScreen

                    if (openInventory) {
                        network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY))
                    }

                    // If there is neither an empty slot nor an empty bowl, then replace whatever there is on slot 9
                    if (!player.inventory.getStack(9).isEmpty || player.inventory.getStack(9).item != Items.BOWL) {
                        utilizeInventory(bowlSlot, 0, SlotActionType.PICKUP, true)
                        utilizeInventory(9, 0, SlotActionType.PICKUP, true)
                        utilizeInventory(bowlSlot, 0, SlotActionType.PICKUP, true)
                    } else {
                        // If there is, simply shift + click the empty bowl from hotbar
                        utilizeInventory(bowlSlot, 0, SlotActionType.QUICK_MOVE, true)
                    }

                    if (openInventory) {
                        network.sendPacket(CloseHandledScreenC2SPacket(0))
                    }
                }
            }
            return@repeatable
        }

        if (player.health < health) {
            if (hotBarSlot != null) {
                if (!saveSlot) {
                    lastSlot = player.inventory.selectedSlot
                    saveSlot = true
                }
                player.inventory.selectedSlot = hotBarSlot
                // Using timer so as to avoid sword shield
                wait(2)
                network.sendPacket(PlayerInteractItemC2SPacket(Hand.MAIN_HAND))
                return@repeatable
            } else {
                // Search for the specific item in inventory and quick move it to hotbar
                if (invSlot != null) {
                    utilizeInventory(invSlot, 0, SlotActionType.QUICK_MOVE, false)
                }
                return@repeatable
            }
        }
    }

    private fun utilizeInventory(slot: Int, button: Int, slotActionType: SlotActionType, onlyActions: Boolean) {
        val serverSlot = convertClientSlotToServerSlot(slot)
        val openInventory = mc.currentScreen !is InventoryScreen

        if (!onlyActions) {
            if (openInventory) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY))
            }
        }

        interaction.clickSlot(0, serverSlot, button, slotActionType, player)

        if (!onlyActions) {
            if (openInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }
        }
    }

    private fun hasItem(slot: Int, item: Item): Boolean {
        return player.inventory.getStack(slot).item == item
    }

    enum class BowlMode(override val choiceName: String) : NamedChoice {
        DROP("Drop"), MOVE("Move")
    }
}
