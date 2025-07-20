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
 *
 *
 */

package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket
import net.minecraft.screen.slot.SlotActionType
import kotlin.math.max
import kotlin.random.Random

/**
 * Manages the inventory state and timings and schedules inventory actions
 *
 * TODO:
 *  - Progress Bar
 *  - Off-screen actions
 */
object InventoryManager : EventListener {

    val isInventoryOpen
        get() = isInInventoryScreen || isInventoryOpenServerSide

    var isInventoryOpenServerSide = false
        internal set(value) {
            if (!field && value) {
                inventoryOpened()
            }
            field = value
        }

    var lastClickedSlot: Int = 0
        private set

    private var recentInventoryOpen = false

    /**
     * As soon the inventory changes unexpectedly,
     * we have to update the scheduled inventory actions
     */
    private var requiresUpdate = false

    /**
     * We keep running during the entire time
     * and schedule the inventory actions
     */
    @Suppress("unused")
    private val repeatingSchedulerExecutor = tickHandler {
        // We are not in-game, so we don't need to do anything and throw away the schedule
        if (!inGame) {
            return@tickHandler
        }

        ModuleDebug.debugParameter(this, "Inventory Open", isInventoryOpen)
        ModuleDebug.debugParameter(this, "Inventory Open Server Side", isInventoryOpenServerSide)

        var maximumCloseDelay = 0

        var cycles = 0
        do {
            cycles++
            // Safety check to prevent infinite loops
            if (cycles > 100) {
                chat("InventoryManager has been running for too long ($cycles cycles) on tick, stopping now. " +
                    "Please report this issue.")
                break
            }

            requiresUpdate = false

            val event = EventManager.callEvent(ScheduleInventoryActionEvent())

            // Schedule of actions that have to be executed
            // The schedule is sorted by
            // 1. With Non-inventory open required actions
            // 2. With inventory open required actions
            val schedule = event.schedule
                .filter { actionChain -> actionChain.canPerformAction() && actionChain.actions.isNotEmpty() }
                .groupBy(InventoryActionChain::requiresInventoryOpen)
                .map { it.value.sortedByDescending { actionChain -> actionChain.priority } }
                .reduceOrNull { acc, inventoryActionChains ->
                    acc + inventoryActionChains
                } ?: break

            ModuleDebug.debugParameter(this, "Schedule Size", schedule.size)

            // If the schedule is empty, we can break the loop
            if (schedule.isEmpty()) {
                break
            }

            // Handle non-inventory open actions first
            for (chained in schedule) {
                // Do not continue if we need to update the schedule
                if (requiresUpdate) {
                    break
                }

                // These are chained actions that have to be executed in order
                // We cannot interrupt them
                for ((index, action) in chained.actions.withIndex()) {
                    val constraints = chained.inventoryConstraints

                    // Update close delay maximum
                    maximumCloseDelay = max(maximumCloseDelay, constraints.closeDelay.random())

                    // First action to be executed will be the start delay
                    if (recentInventoryOpen) {
                        recentInventoryOpen = false
                        waitTicks(constraints.startDelay.random())
                        cycles = 0
                    }

                    // Handle player inventory open requirements
                    val requiresPlayerInventory = action.requiresPlayerInventoryOpen()
                    if (requiresPlayerInventory) {
                        if (!isInventoryOpen) {
                            openInventorySilently()
                            waitTicks(constraints.startDelay.random())
                            cycles = 0
                        }
                    } else if (canCloseMainInventory) {
                        // When all scheduled actions are done, we can close the inventory
                        if (isInventoryOpen) {
                            waitTicks(constraints.closeDelay.random())
                            cycles = 0
                            closeInventorySilently()
                        }
                    }

                    // This should usually not happen, but we have to check it
                    if (!chained.canPerformAction()) {
                        logger.warn("Cannot perform action $action because it is not possible")
                        break
                    }

                    // Check if this is the first action in the chain, which allows us to simulate a miss click
                    // This is only possible for container-type slots and also does not make much sense when
                    // the action is a throw action (you cannot miss-click really when throwing)
                    if (index == 0 && action is ClickInventoryAction
                        && constraints.missChance.random() > Random.nextInt(100)
                        && action.actionType != SlotActionType.THROW) {
                        // Simulate a miss click (this is only possible for container-type slots)
                        // TODO: Add support for inventory slots
                        if (action.performMissClick()) {
                            waitTicks(constraints.clickDelay.random())
                            cycles = 0
                        }
                    }

                    if (action is CloseContainerAction) {
                        waitTicks(constraints.closeDelay.random())
                        cycles = 0
                    }
                    if (action.performAction()) {
                        if (action !is CloseContainerAction) {
                            waitTicks(constraints.clickDelay.random())
                            cycles = 0
                        }
                    }
                }
            }
        } while (schedule.isNotEmpty())

        // When all scheduled actions are done, we can close the inventory
        if (isInventoryOpen && canCloseMainInventory) {
            waitTicks(maximumCloseDelay)
            closeInventorySilently()
        }

        lastClickedSlot = 0
    }

    /**
     * Called when a click occurred. Can be tracked by listening for [ClickSlotC2SPacket]
     */
    @JvmStatic
    fun clickOccurred() {
        // Every click will require an update
        requiresUpdate = true
    }

    /**
     * Called when the inventory was opened. Can be tracked by listening for [OpenScreenS2CPacket]
     */
    @JvmStatic
    fun inventoryOpened() {
        recentInventoryOpen = true
    }

    /**
     * Listener for packets that are related to the inventory
     * to keep track of the inventory state and timings
     */
    val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val packet = event.packet

        if (event.isCancelled) {
            return@handler
        }

        // If we actually send a click packet, we can reset the click chronometer
        if (packet is ClickSlotC2SPacket) {
            clickOccurred()

            if (packet.syncId == 0) {
                isInventoryOpenServerSide = true
            }
        }

        if (packet is CloseHandledScreenC2SPacket || packet is CloseScreenS2CPacket || packet is OpenScreenS2CPacket) {
            // Prevent closing inventory (no other screen!) if it is already closed
            if (!isInventoryOpenServerSide && packet is CloseHandledScreenC2SPacket && packet.syncId == 0) {
                event.cancelEvent()
                return@handler
            }

            isInventoryOpenServerSide = false
        }
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent>(
        priority = EventPriorityConvention.READ_FINAL_STATE
    ) { event ->
        val screen = event.screen

        if (event.isCancelled) {
            return@handler
        }

        if (screen is InventoryScreen || screen is GenericContainerScreen) {
            // ViaFabricPlus injects into [tutorialManager.onInventoryOpened()] but we take
            // the easy way and just listen for the screen event.
            if (screen is InventoryScreen && isOlderThanOrEqual1_11_1) {
                isInventoryOpenServerSide = true
            }

            inventoryOpened()
        }
    }

    @Suppress("unused")
    private val handleWorldChange = handler<WorldChangeEvent> {
        isInventoryOpenServerSide = false
    }

}

interface InventoryAction {
    fun canPerformAction(inventoryConstraints: InventoryConstraints): Boolean
    fun performAction(): Boolean
    fun requiresPlayerInventoryOpen(): Boolean
}

data class ClickInventoryAction(
    val screen: GenericContainerScreen? = null,
    val slot: ItemSlot,
    val button: Int,
    val actionType: SlotActionType,
) : InventoryAction {

    companion object {

        fun click(screen: GenericContainerScreen? = null,
                  slot: ItemSlot,
                  button: Int,
                  actionType: SlotActionType) = ClickInventoryAction(
            screen,
            slot = slot,
            button = button,
            actionType = actionType
        )

        fun performThrow(
            screen: GenericContainerScreen? = null,
            slot: ItemSlot
        ) = ClickInventoryAction(
            screen,
            slot = slot,
            button = 1,
            actionType = SlotActionType.THROW
        )

        fun performQuickMove(
            screen: GenericContainerScreen? = null,
            slot: ItemSlot
        ) = ClickInventoryAction(
            screen,
            slot = slot,
            button = 0,
            actionType = SlotActionType.QUICK_MOVE
        )

        fun performSwap(
            screen: GenericContainerScreen? = null,
            from: ItemSlot,
            to: HotbarItemSlot
        ) = ClickInventoryAction(
            screen,
            slot = from,
            button = to.hotbarSlotForServer,
            actionType = SlotActionType.SWAP
        )

        fun performPickupAll(
            screen: GenericContainerScreen? = null,
            slot: ItemSlot
        ) = ClickInventoryAction(
            screen,
            slot = slot,
            button = 0,
            actionType = SlotActionType.PICKUP_ALL
        )

        fun performPickup(
            screen: GenericContainerScreen? = null,
            slot: ItemSlot
        ) = ClickInventoryAction(
            screen,
            slot = slot,
            button = 0,
            actionType = SlotActionType.PICKUP
        )

    }

    override fun canPerformAction(inventoryConstraints: InventoryConstraints): Boolean {
        // Check constrains
        if (!inventoryConstraints.passesRequirements(this)) {
            return false
        }

        // Screen is null, which means we are targeting the player inventory
        if (requiresPlayerInventoryOpen() && player.currentScreenHandler.isPlayerInventory &&
            !interaction.hasRidingInventory()) {
            return true
        }

        // Check if current screen is the same as the screen we want to interact with
        val screen = mc.currentScreen as? GenericContainerScreen ?: return false
        return screen.syncId == this.screen.syncId
    }

    override fun performAction(): Boolean {
        val slotId = slot.getIdForServer(screen) ?: return false
        interaction.clickSlot(screen?.syncId ?: 0, slotId, button, actionType, player)

        return true
    }

    fun performMissClick(): Boolean {
        if (slot !is ContainerItemSlot || screen == null) {
            return false
        }

        val itemsInContainer = getSlotsInContainer(screen)
        // Find the closest item to the slot which is empty
        val closestEmptySlot = itemsInContainer
            .filter { it.itemStack.isEmpty }
            .minByOrNull { slot.distance(it) } ?: return false

        interaction.clickSlot(screen.syncId, closestEmptySlot.getIdForServer(screen), 0,
            SlotActionType.PICKUP, player)
        return true
    }

    override fun requiresPlayerInventoryOpen() = screen == null

}

data class UseInventoryAction(
    val hotbarItemSlot: HotbarItemSlot
) : InventoryAction {

    override fun canPerformAction(inventoryConstraints: InventoryConstraints) =
        !InventoryManager.isInventoryOpen && !isInContainerScreen && !isInInventoryScreen

    override fun performAction(): Boolean {
        useHotbarSlotOrOffhand(hotbarItemSlot)
        return true
    }

    override fun requiresPlayerInventoryOpen() = false

}

data class CloseContainerAction(
    val screen: GenericContainerScreen
) : InventoryAction {

    // Check if current handler is the same as the screen we want to close
    override fun canPerformAction(inventoryConstraints: InventoryConstraints) =
        player.currentScreenHandler.syncId == screen.syncId

    override fun performAction(): Boolean {
        player.closeHandledScreen()
        return true
    }

    override fun requiresPlayerInventoryOpen() = false

}

data class CreativeInventoryAction(
    val itemStack: ItemStack,
    val slot: ItemSlot? = null
) : InventoryAction {

    companion object {
        fun performThrow(itemStack: ItemStack) = CreativeInventoryAction(itemStack)
        fun performFillSlot(itemStack: ItemStack, slot: ItemSlot) = CreativeInventoryAction(itemStack, slot)
    }

    override fun canPerformAction(inventoryConstraints: InventoryConstraints): Boolean {
        // Check constrains
        if (!inventoryConstraints.passesRequirements(this)) {
            return false
        }

        // Screen is null, which means we are targeting the player inventory
        if (requiresPlayerInventoryOpen() && player.currentScreenHandler.isPlayerInventory &&
            !interaction.hasRidingInventory()) {
            return true
        }

        return player.isCreative
    }

    override fun performAction(): Boolean {
        val slot = slot

        if (slot != null) {
            val slotId = slot.getIdForServer(null) ?: return false
            interaction.clickCreativeStack(itemStack, slotId)
        } else {
            interaction.dropCreativeStack(itemStack)
        }
        return true
    }

    override fun requiresPlayerInventoryOpen() = false

}

/**
 * A chained inventory action is a list of inventory actions that have to be executed in order
 * and CANNOT be stopped in between
 */
data class InventoryActionChain(
    val inventoryConstraints: InventoryConstraints,
    val actions: Array<out InventoryAction>,
    val priority: Priority
) {

    fun canPerformAction(): Boolean {
        return actions.all { action -> action.canPerformAction(inventoryConstraints) }
    }

    fun requiresInventoryOpen() = actions.filterIsInstance<ClickInventoryAction>().any { it.screen == null }

}
