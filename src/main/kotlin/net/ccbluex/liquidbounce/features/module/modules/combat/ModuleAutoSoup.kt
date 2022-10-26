/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoGapple.utilizeInventory
import net.ccbluex.liquidbounce.utils.item.InventoryConstraintsConfigurable
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
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
    private val health by int("Health", 15, 1..20)
    private val inventoryConstraints = tree(InventoryConstraintsConfigurable())

    val repeatable = repeatable {
        val mushroomStewSlot = findHotbarSlot(Items.MUSHROOM_STEW)
        val bowlHotbarSlot = findHotbarSlot(Items.BOWL)
        val bowlInvSlot = findInventorySlot(Items.MUSHROOM_STEW)

        if (mushroomStewSlot == null && bowlInvSlot == null && bowlHotbarSlot == null) {
            return@repeatable
        }

        if (bowlHotbarSlot != null) {
            network.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN
                )
            )
            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
            when (bowl) {
                BowlMode.DROP -> {
                    utilizeInventory(bowlHotbarSlot, 1, SlotActionType.THROW, inventoryConstraints)
                }
                BowlMode.MOVE -> {
                    // If there is neither an empty slot nor an empty bowl, then replace whatever there is on slot 9
                    if (!player.inventory.getStack(9).isEmpty || player.inventory.getStack(9).item != Items.BOWL) {
                        utilizeInventory(bowlHotbarSlot, 0, SlotActionType.PICKUP, inventoryConstraints, false)
                        utilizeInventory(9, 0, SlotActionType.PICKUP, inventoryConstraints, false)
                        utilizeInventory(bowlHotbarSlot, 0, SlotActionType.PICKUP, inventoryConstraints)
                    } else {
                        // If there is, simply shift + left-click the empty bowl from hotbar
                        utilizeInventory(bowlHotbarSlot, 0, SlotActionType.QUICK_MOVE, inventoryConstraints)
                    }
                }
            }
            return@repeatable
        }

        if (player.health < health) {
            if (mushroomStewSlot != null) {
                wait { inventoryConstraints.delay.random() }

                if (mushroomStewSlot != player.inventory.selectedSlot) {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(mushroomStewSlot))
                }

                if (player.isBlocking) {
                    waitUntil { !player.isBlocking }
                }

                network.sendPacket(PlayerInteractItemC2SPacket(Hand.MAIN_HAND))
                return@repeatable
            } else {
                // Search for the specific item in inventory and quick move it to hotbar
                if (bowlInvSlot != null) {
                    utilizeInventory(bowlInvSlot, 0, SlotActionType.QUICK_MOVE, inventoryConstraints)
                }
                return@repeatable
            }
        }
    }

    enum class BowlMode(override val choiceName: String) : NamedChoice {
        DROP("Drop"), MOVE("Move")
    }
}
