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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoTotem.Health.doesNotPassHealth
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.ClickInventoryAction
import net.ccbluex.liquidbounce.utils.inventory.OFFHAND_SLOT
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.minecraft.item.Items

/**
 * AutoTotem module
 *
 * Automatically places a totem in off-hand.
 */
object ModuleAutoTotem : Module("AutoTotem", Category.PLAYER) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    private object Health : ToggleableConfigurable(this, "Health", true) {

        val targetHealth by int("Health", 18, 0..20)

        val doesNotPassHealth: Boolean
            get() = Health.enabled && player.health > targetHealth

    }

    private val swapMode by enumChoice("Mode", SwapTotemMode.OFFHANDPICKUP)

    /**
     * Returns totem slot if everything passes - this is useful for InventoryCleaner to prepare and avoid
     * conflicts
     */
    val totemSlot: ItemSlot?
        get() {
            if (!enabled) {
                return null
            }

            // Player cannot die
            if (player.isCreative || player.isSpectator || player.isDead) {
                return null
            }

            // Does not meet criteria
            if (doesNotPassHealth) {
                return null
            }

            if (player.offHandStack.item == Items.TOTEM_OF_UNDYING) {
                return OFFHAND_SLOT
            }

            return findInventorySlot { it.item == Items.TOTEM_OF_UNDYING }
        }

    init {
        tree(Health)
    }

    @Suppress("unused")
    private val autoTotemHandler = handler<ScheduleInventoryActionEvent> {
        val slot = totemSlot ?: return@handler

        // Totem is already located in Off-hand slot
        if (totemSlot == OFFHAND_SLOT) {
            return@handler
        }

        val action = when (swapMode) {
            SwapTotemMode.OFFHANDPICKUP -> {
                val isOffhandEmpty = OffHandSlot.itemStack.isEmpty

                listOfNotNull(
                    ClickInventoryAction.performPickup(slot = slot),
                    ClickInventoryAction.performPickup(slot = OffHandSlot),

                    if (!isOffhandEmpty) ClickInventoryAction.performPickup(slot = slot) else null,
                )
            }

            SwapTotemMode.SWAP -> {
                listOf(
                    ClickInventoryAction.performSwap(from = slot, to = OffHandSlot)
                )
            }

            SwapTotemMode.HOTBARPICKUP -> {
                val activeSlot = HotbarItemSlot(player.inventory.swappableHotbarSlot)
                val isSlotEmpty = activeSlot.itemStack.isEmpty
                val isOffhandEmpty = OffHandSlot.itemStack.isEmpty

                listOfNotNull(
                    ClickInventoryAction.performPickup(slot = slot),
                    ClickInventoryAction.performPickup(slot = activeSlot),

                    if (!isSlotEmpty) ClickInventoryAction.performPickup(slot = slot) else null,

                    ClickInventoryAction.performPickup(slot = activeSlot),
                    ClickInventoryAction.performPickup(slot = OffHandSlot),

                    if (!isOffhandEmpty) ClickInventoryAction.performPickup(slot = activeSlot) else null,

                    if (!isSlotEmpty) ClickInventoryAction.performPickup(slot = slot) else null,
                    if (!isSlotEmpty) ClickInventoryAction.performPickup(slot = activeSlot) else null,

                    if (!isOffhandEmpty && !isSlotEmpty) ClickInventoryAction.performPickup(slot = slot) else null,
                )
            }
        }
        it.schedule(inventoryConstraints, action)
    }

    enum class SwapTotemMode(override val choiceName: String) : NamedChoice {
        SWAP("Swap"),
        OFFHANDPICKUP("OffhandPickup"),
        HOTBARPICKUP("HotbarPickup"),
    }
}
