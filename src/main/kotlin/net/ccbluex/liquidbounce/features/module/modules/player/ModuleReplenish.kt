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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction.Click
import net.ccbluex.liquidbounce.utils.inventory.InventoryItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isMergeable
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Module Replenish
 *
 * Automatically refills your hotbar with items from your inventory when the count drops to a certain threshold.
 *
 * @author ccetl
 */
object ModuleReplenish : ClientModule("Replenish", ModuleCategories.PLAYER, aliases = listOf("Refill")) {
    private val constraints = tree(PlayerInventoryConstraints())
    private val itemThreshold by int("ItemThreshold", 5, 0..63)
    private val delay by int("Delay", 40, 0..1000, "ms")
    private val features by multiEnumChoice("Features", Features.CLEANUP)
    private val insideOf by multiEnumChoice<InsideOf>("InsideOf")

    // 0..9 -> hotbar 10 -> offHand
    private val trackedHotbarItems = Array(10) { Items.AIR }
    private val chronometer = Chronometer()

    private fun clear() {
        trackedHotbarItems.fill(Items.AIR)
    }

    override fun onEnabled() {
        clear()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        clear()
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        if (event.screen is AbstractContainerScreen<*>) {
            clear()
        }
    }

    @Suppress("unused")
    private val inventoryScheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (!chronometer.hasElapsed(delay.toLong()) || !player.containerMenu.carried.isEmpty) {
            return@handler
        }

        chronometer.reset()

        Slots.OffhandWithHotbar.slots.forEach { slot ->
            val idx = if (slot is OffHandSlot) trackedHotbarItems.lastIndex else slot.hotbarSlot

            // find the desired item
            val itemStack = slot.itemStack.takeUnless { it.isEmpty } ?: ItemStack(trackedHotbarItems[idx], 0)
            if (itemStack.isEmpty) {
                return@forEach
            }

            val currentStackNotEmpty = !itemStack.isEmpty

            // check if the current stack, if not empty, is allowed to be refilled
            val unsupportedStackSize = itemStack.maxStackSize <= itemThreshold
            if (currentStackNotEmpty && (unsupportedStackSize || itemStack.count > itemThreshold)) {
                trackedHotbarItems[idx] = itemStack.item
                return@forEach
            }

            // find replacement items
            val inventorySlots = Slots.Inventory.slots
                .filterTo(mutableListOf()) { it.itemStack.isMergeable(itemStack) }

            // no stack to refill found
            if (inventorySlots.isEmpty()) {
                trackedHotbarItems[idx] = itemStack.item
                return@forEach
            }

            inventorySlots.sortWith(ItemSlot.PREFER_MORE_ITEM)
            val slotWithMaxCount = inventorySlots.first()

            if (Features.CLEANUP in features) {
                // clean up small stacks first when cleanUp is enabled otherwise prioritize larger stacks
                inventorySlots.reverse()
            }

            // refill
            when {
                Features.USE_SWAP in features &&
                    slotWithMaxCount.itemStack.count.let { it > itemStack.count && it > itemThreshold } ->
                    event.schedule(
                        constraints,
                        Click.performSwap(from = slotWithMaxCount, to = slot)
                    )

                Features.USE_PICKUP_ALL in features && currentStackNotEmpty ->
                    event.schedule(
                        constraints,
                        Click.performMergeStack(slot = slot),
                    )

                else -> event.scheduleNormalRefill(
                    itemStack,
                    if (currentStackNotEmpty) itemStack.count else 0,
                    inventorySlots,
                    slot,
                )
            }

            trackedHotbarItems[idx] = itemStack.item
            return@handler
        }
    }

    private fun ScheduleInventoryActionEvent.scheduleNormalRefill(
        itemStack: ItemStack,
        count: Int,
        inventorySlots: List<InventoryItemSlot>,
        slot: HotbarItemSlot,
    ) {
        var neededToRefill = itemStack.maxStackSize - count
        for (inventorySlot in inventorySlots) {
            neededToRefill -= inventorySlot.itemStack.count
            val actions = ArrayList<Click>(3)
            actions += Click.performPickup(slot = inventorySlot)
            actions += Click.performPickup(slot = slot)

            if (neededToRefill < 0) {
                actions += Click.performPickup(slot = slot)
            }

            schedule(constraints, actions)

            if (neededToRefill <= 0) {
                break
            }
        }
    }

    override val running: Boolean
        get() = super.running &&
            (InsideOf.CHESTS in insideOf
                || (mc.screen !is AbstractContainerScreen<*>
                || mc.screen is InventoryScreen)
                ) &&
            (InsideOf.INVENTORIES in insideOf
                || mc.screen !is InventoryScreen
                )

    private enum class Features(
        override val tag: String
    ) : Tagged {
        CLEANUP("CleanUp"),
        USE_PICKUP_ALL("UsePickupAll"),
        USE_SWAP("UseSwap"),
    }

    @Suppress("unused")
    private enum class InsideOf(
        override val tag: String
    ) : Tagged {
        CHESTS("Chests"),
        INVENTORIES("Inventories")
    }
}
