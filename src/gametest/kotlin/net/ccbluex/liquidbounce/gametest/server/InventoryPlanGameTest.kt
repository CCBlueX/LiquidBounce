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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryPlan
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.inventoryPlan
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An [InventoryPlan] on a real server: it is kept across its own clicks, made again when something else changes the
 * inventory, waits for a plan of a higher priority, and takes the cursor to what it clicks.
 */
class InventoryPlanGameTest : ServerGameTest({
    keepsPlanAcrossClicks()
    plansAgainAfterServerChange()
    performsHigherPriorityFirst()
    movesCursorToClickedSlot()
})

/**
 * A plan that swaps each of [moves]' items from the inventory to its hotbar slot, waiting [pause] ticks after each.
 */
private class Sorter(
    private val moves: Map<Item, HotbarItemSlot>,
    private val priority: Priority = Priority.NORMAL,
    private val pause: Int = 0,
    private val performed: MutableList<Item> = CopyOnWriteArrayList(),
) : EventListener {

    @Volatile
    var isActive = false

    /** How often it decided on its swaps. */
    @Volatile
    var plans = 0

    override val running
        get() = isActive

    @Suppress("unused")
    private val sort = inventoryPlan(PlayerInventoryConstraints()) {
        val swaps = moves.mapNotNull { (item, to) -> Slots.Inventory.findSlot(item)?.let { Triple(item, it, to) } }
        if (swaps.isEmpty()) {
            return@inventoryPlan
        }

        plans++
        for ((item, from, to) in swaps) {
            perform(InventoryAction.Click.performSwap(null, slotOf(from), to), priority = priority)
            performed += item
            waitTicks(pause)
        }
    }
}

private inline fun GameTestScope.withSorters(vararg sorters: Sorter, block: () -> Unit) {
    try {
        block()
    } finally {
        client { sorters.forEach { it.isActive = false; it.unregister() } }
    }
}

private fun GameTestScope.movesCursorToClickedSlot() {
    giveIngots()
    val sorter = client { Sorter(INGOT_SLOTS) }
    withSorters(sorter) {
        enable(ModuleBetterInventory)
        client { it.gui.setScreen(InventoryScreen(checkNotNull(it.player))) }
        try {
            sorter.isActive = true
            awaitIngotsSorted()
            val isOnSlot = client { mc ->
                val screen = mc.gui.screen() as InventoryScreen
                // inventory.11, where the ingot swapped last was
                val slot = screen.menu.slots[20]
                val x = mc.mouseHandler.xpos() * mc.window.guiScaledWidth / mc.window.screenWidth - screen.leftPos
                val y = mc.mouseHandler.ypos() * mc.window.guiScaledHeight / mc.window.screenHeight - screen.topPos
                x >= slot.x && x <= slot.x + 16 && y >= slot.y && y <= slot.y + 16
            }
            check(isOnSlot) { "the cursor did not end on the slot clicked last" }
        } finally {
            client { it.gui.setScreen(null) }
            disable(ModuleBetterInventory)
        }
    }
}

private fun GameTestScope.giveIngots() {
    run("clear $playerName")
    run("execute at $playerName run kill @e[type=item,distance=..8]")
    awaitClient("an empty inventory") { checkNotNull(it.player).inventory.isEmpty }
    setSlot("inventory.9", "minecraft:iron_ingot")
    setSlot("inventory.10", "minecraft:gold_ingot")
    setSlot("inventory.11", "minecraft:copper_ingot")
    awaitClient("the ingots") { checkNotNull(it.player).inventory.countItem(Items.COPPER_INGOT) == 1 }
}

private fun GameTestScope.setSlot(slot: String, item: String) {
    run("item replace entity $playerName $slot with $item")
}

private fun GameTestScope.awaitIngotsSorted() {
    awaitServer("items entity $playerName hotbar.4 minecraft:iron_ingot")
    awaitServer("items entity $playerName hotbar.5 minecraft:gold_ingot")
    awaitServer("items entity $playerName hotbar.6 minecraft:copper_ingot")
}

private val INGOT_SLOTS = mapOf(
    Items.IRON_INGOT to HotbarItemSlot.SLOT_4,
    Items.GOLD_INGOT to HotbarItemSlot.SLOT_5,
    Items.COPPER_INGOT to HotbarItemSlot.SLOT_6,
)

private fun GameTestScope.keepsPlanAcrossClicks() {
    giveIngots()
    val sorter = client { Sorter(INGOT_SLOTS) }
    withSorters(sorter) {
        sorter.isActive = true
        awaitIngotsSorted()
        check(sorter.plans == 1) { "three swaps took ${sorter.plans} plans instead of one" }
    }
}

private fun GameTestScope.plansAgainAfterServerChange() {
    giveIngots()
    val sorter = client { Sorter(INGOT_SLOTS, pause = 40) }
    withSorters(sorter) {
        sorter.isActive = true
        awaitClient("the first swap") { checkNotNull(it.player).inventory.getItem(4).`is`(Items.IRON_INGOT) }
        setSlot("inventory.20", "minecraft:stick")
        awaitClient("a new plan") { sorter.plans == 2 }
        awaitIngotsSorted()
    }
}

private fun GameTestScope.performsHigherPriorityFirst() {
    giveIngots()
    val performed = CopyOnWriteArrayList<Item>()
    val low = client {
        Sorter(INGOT_SLOTS.filterKeys { it !== Items.COPPER_INGOT }, performed = performed)
    }
    val high = client {
        Sorter(
            INGOT_SLOTS.filterKeys { it === Items.COPPER_INGOT },
            Priority.IMPORTANT_FOR_USAGE_1,
            performed = performed,
        )
    }
    withSorters(low, high) {
        client {
            low.isActive = true
            high.isActive = true
        }
        awaitIngotsSorted()
        check(performed.first() === Items.COPPER_INGOT) { "the lower priority went first: $performed" }
        check(low.plans == 2) { "the other plan's click left the lower one with its ${low.plans} plan(s)" }
    }
}
