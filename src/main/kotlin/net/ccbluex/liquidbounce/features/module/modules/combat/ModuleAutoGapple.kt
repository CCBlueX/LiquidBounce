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
import net.ccbluex.liquidbounce.utils.client.pressedOnKeyboard
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType

/**
 * AutoGapple module
 *
 * Automatically eats apples whenever your health is low.
 */

object ModuleAutoGapple : Module("AutoGapple", Category.COMBAT) {

    private val health by int("Health", 15, 1..20)
    private val inventoryConstraints = tree(InventoryConstraintsConfigurable())

    private var lastSlot = -1

    val repeatable = repeatable {
        val slot = findHotbarSlot(Items.GOLDEN_APPLE)
        val invSlot = findInventorySlot(Items.GOLDEN_APPLE)

        if (slot == null && invSlot == null || interaction.hasRidingInventory()) {
            if (lastSlot != -1) {
                player.inventory.selectedSlot = lastSlot
                lastSlot = -1
            }

            return@repeatable
        }

        if (player.health + player.absorptionAmount < health) {
            if (slot != null) {
                wait { inventoryConstraints.delay.random() }

                if (slot != player.inventory.selectedSlot) {
                    lastSlot = player.inventory.selectedSlot
                    player.inventory.selectedSlot = slot
                }

                if (player.isBlocking) {
                    waitUntil { !player.isBlocking }
                }

                mc.options.useKey.isPressed = true

                waitUntil { player.health + player.absorptionAmount >= health }

                mc.options.useKey.isPressed = false

                if (lastSlot != -1) {
                    player.inventory.selectedSlot = lastSlot
                    lastSlot = -1
                }

                return@repeatable
            } else if (invSlot != null && (0..8).any { player.inventory.getStack(it).isEmpty }) {
                utilizeInventory(invSlot, 0, SlotActionType.QUICK_MOVE, inventoryConstraints)

                return@repeatable
            }
        }
    }

    fun utilizeInventory(
        item: Int,
        button: Int,
        slotActionType: SlotActionType,
        inventoryConstraints: InventoryConstraintsConfigurable,
        close: Boolean = true,
    ) {
        val slot = convertClientSlotToServerSlot(item)

        if (!isInInventoryScreen) {
            openInventorySilently()
        }

        if (!inventoryConstraints.violatesNoMove && (!inventoryConstraints.invOpen || isInInventoryScreen)) {
            interaction.clickSlot(0, slot, button, slotActionType, player)

            if (close) {
                if (!isInInventoryScreen) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }
            }
        }
    }

    override fun disable() {
        if (!mc.options.useKey.pressedOnKeyboard) {
            mc.options.useKey.isPressed = false
        }

        if (lastSlot != -1) {
            player.inventory.selectedSlot = lastSlot
            lastSlot = -1
        }
    }
}
