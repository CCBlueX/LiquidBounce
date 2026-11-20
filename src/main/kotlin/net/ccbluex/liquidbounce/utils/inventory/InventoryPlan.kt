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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.utils.inventory

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.activeGuards
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack

/**
 * Runs [plan] on every tick no run of it is active. See [InventoryPlan].
 */
fun EventListener.inventoryPlan(
    constraints: InventoryConstraints,
    plan: suspend InventoryPlan.() -> Unit,
): EventHook<GameTickEvent> = tickHandler { InventoryPlan(this@inventoryPlan, constraints).plan() }

/**
 * One run of a module's inventory work: it reads the inventory, decides on all its steps and performs them one after
 * another.
 *
 * From the first [perform] on, the run requires the open menu to hold what the plan expects: what it held then, plus
 * what the plan's own clicks did since. A change from anything else (the server putting an item back, a pickup,
 * another plan, the player) ends the run at its next suspension and the next tick starts a new one, which plans again.
 * So does the server locking an item. Whatever else the steps depend on is to be required by the plan itself.
 */
class InventoryPlan internal constructor(
    internal val listener: EventListener,
    val constraints: InventoryConstraints,
) {

    private class Expected(
        val containerId: Int,
        val stacks: List<ItemStack>,
        val carried: ItemStack,
        val serverLockedCount: Int,
    )

    private var expected: Expected? = null

    /** Slot a stack was in at the start to the slot this plan's swaps moved it to. */
    private val moved = HashMap<ItemSlot, ItemSlot>()

    /**
     * Performs [actions] as one chain that is not interrupted, as soon as they are possible and no chain of a higher
     * [priority] is waiting. Returns right after the last of them was sent; the click delay passes before the next
     * chain is performed.
     */
    suspend fun perform(vararg actions: InventoryAction, priority: Priority = Priority.NORMAL) =
        perform(actions.asList(), priority)

    suspend fun perform(actions: List<InventoryAction>, priority: Priority = Priority.NORMAL) {
        if (actions.isEmpty()) {
            return
        }

        if (expected == null) {
            expectCurrent()
            requires { matchesExpected() }
        }

        val chain = InventoryAction.Chain(constraints, actions, priority)
        val guards = currentCoroutineContext().activeGuards()
        suspendCancellableCoroutine { continuation ->
            InventoryManager.request(this, chain, guards, continuation)
        }
    }

    /**
     * Where the stack that was in [slot] when the plan started is now, after the swaps this plan performed.
     */
    fun slotOf(slot: ItemSlot): ItemSlot = moved[slot] ?: slot

    /**
     * Called right after [action] was sent, while the client shows what it predicts the server to do.
     */
    internal fun onPerformed(action: InventoryAction) {
        expectCurrent()

        if (action is InventoryAction.Click && action.actionType == ContainerInput.SWAP) {
            val target = HotbarItemSlot.entries.find { it.inventorySlot == action.button } ?: return
            val first = originOf(action.slot)
            val second = originOf(target)
            moved[first] = target
            moved[second] = action.slot
        }
    }

    private fun originOf(slot: ItemSlot) = moved.entries.find { it.value == slot }?.key ?: slot

    private fun expectCurrent() {
        val menu = player.containerMenu
        expected = Expected(
            menu.containerId,
            menu.slots.map { it.item.copy() },
            menu.carried.copy(),
            InventoryManager.serverLockedCount,
        )
    }

    private fun matchesExpected(): Boolean {
        val menu = player.containerMenu
        val expected = expected ?: return true

        return menu.containerId == expected.containerId &&
            InventoryManager.serverLockedCount == expected.serverLockedCount &&
            menu.slots.size == expected.stacks.size &&
            ItemStack.matches(menu.carried, expected.carried) &&
            menu.slots.indices.all { ItemStack.matches(menu.slots[it].item, expected.stacks[it]) }
    }

}
