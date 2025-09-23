/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction.Click
import net.ccbluex.liquidbounce.utils.inventory.InventoryItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isMergeable
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

/**
 * Module Replenish
 *
 * Automatically refills your hotbar with items from your inventory when the count drops to a certain threshold.
 *
 * @author ccetl
 */
@Suppress("MagicNumber")
object ModuleReplenish : ClientModule("Replenish", Category.PLAYER, aliases = listOf("Refill")) {
    private val constraints = tree(PlayerInventoryConstraints())
    private val itemThreshold by int("ItemThreshold", 5, 0..63)
    private val delay by int("Delay", 40, 0..1000, "ms")
    private val features by multiEnumChoice("Features", Features.CLEANUP)
    private val insideOf by multiEnumChoice<InsideOf>("InsideOf")

    // 0..9 -> hotbar 10 -> offHand
    private val trackedHotbarItems = Array<Item>(10) { Items.AIR }
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
        if (event.screen is HandledScreen<*>) {
            clear()
        }
    }

    @Suppress("unused")
    private val inventoryScheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (!chronometer.hasElapsed(delay.toLong())) {
            return@handler
        }

        chronometer.reset()

        Slots.OffhandWithHotbar.slots.forEach { slot ->
            val itemStack = slot.itemStack
            val idx = if (slot is OffHandSlot) trackedHotbarItems.lastIndex else slot.hotbarSlot

            // find the desired item
            val item = itemStack.item.takeUnless { it == Items.AIR } ?: trackedHotbarItems[idx]
            if (item == Items.AIR) {
                return@forEach
            }

            val currentStackNotEmpty = !itemStack.isEmpty

            // check if the current stack, if not empty, is allowed to be refilled
            val unsupportedStackSize = itemStack.maxCount <= itemThreshold
            if (currentStackNotEmpty && (unsupportedStackSize || itemStack.count > itemThreshold)) {
                trackedHotbarItems[idx] = itemStack.item
                return@forEach
            }

            // find replacement items
            val inventorySlots = Slots.Inventory.slots
                .filter { it.itemStack.isMergeable(itemStack) }
                .sortedWith(
                    // clean up small stacks first when cleanUp is enabled otherwise prioritize larger stacks
                    if (Features.CLEANUP in features) {
                        compareBy {
                            it.itemStack.count
                        }
                    } else {
                        compareByDescending {
                            it.itemStack.count
                        }
                    }
                )

            // no stack to refill found
            if (inventorySlots.isEmpty()) {
                trackedHotbarItems[idx] = itemStack.item
                return@forEach
            }

            // refill
            if (Features.USE_PICKUP_ALL in features && currentStackNotEmpty) {
                event.schedule(
                    constraints,
                    Click.performMergeStack(slot = slot),
                )
            } else {
                refillNormal(itemStack, if (currentStackNotEmpty) itemStack.count else 0, inventorySlots, slot, event)
            }

            trackedHotbarItems[idx] = item
            return@handler
        }
    }

    private fun refillNormal(
        itemStack: ItemStack,
        count: Int,
        inventorySlots: List<InventoryItemSlot>,
        slot: HotbarItemSlot,
        event: ScheduleInventoryActionEvent
    ) {
        var neededToRefill = itemStack.maxCount - count
        inventorySlots.forEach { inventorySlot ->
            neededToRefill -= inventorySlot.itemStack.count
            val actions = ArrayList<Click>(3)
            actions += Click.performPickup(slot = inventorySlot)
            actions += Click.performPickup(slot = slot)

            if (neededToRefill < 0) {
                actions += Click.performPickup(slot = slot)
            }

            event.schedule(constraints, actions)

            if (neededToRefill <= 0) {
                return
            }
        }
    }

    override val running: Boolean
        get() = super.running &&
            (InsideOf.CHESTS in insideOf
                || (mc.currentScreen !is HandledScreen<*>
                || mc.currentScreen is InventoryScreen)
            ) &&
            (InsideOf.INVENTORIES in insideOf
                || mc.currentScreen !is InventoryScreen
            )

    private enum class Features(
        override val choiceName: String
    ) : NamedChoice {
        CLEANUP("CleanUp"),
        USE_PICKUP_ALL("UsePickupAll")
    }

    @Suppress("unused")
    private enum class InsideOf(
        override val choiceName: String
    ) : NamedChoice {
        CHESTS("Chests"),
        INVENTORIES("Inventories")
    }
}
