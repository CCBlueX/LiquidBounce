/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket
import net.minecraft.screen.slot.SlotActionType

/**
 * Manages the inventory state and timings and schedules inventory actions
 *
 * TODO:
 *  - Simulate miss clicks between actions
 *  - Progress Bar
 *  - Off-screen actions
 */
object InventoryManager : Listenable {

    private val openSinceChronometer = Chronometer()
    private val clickChronometer = Chronometer()

    var isInventoryOpenServerSide = false
        internal set

    var lastClickedSlot: Int = 0
        private set

    var hasQueue = false
        private set

    /**
     * We keep running during the entire time
     * and schedule the inventory actions
     */
    private val tickHandler = handler<GameTickEvent> {
        // We are not in-game, so we don't need to do anything and throw away the schedule
        if (!inGame) {
            return@handler
        }

        var timeElapsed = clickChronometer.elapsed

        while (timeElapsed > 0) {
            val action = EventManager.callEvent(ScheduleInventoryActionEvent())
                .actions
                .filter {
                    openSinceChronometer.hasElapsed(it.inventoryConstraints.startDelay.random().toLong())
                        && it.canPerformAction()
                }.minByOrNull { it.priority.priority }

            // Resets click chronometer if no action was found or queue changed
            val queueChanged = hasQueue
            hasQueue = action != null
            if (action == null || !queueChanged) {
                clickChronometer.reset()
                break
            }

            // Check if we can perform the action
            val time = action.inventoryConstraints.clickDelay.random()
            if (timeElapsed < time) {
                break
            }

            action.performAction()
            chat("Performed action: $action time elapsed: $timeElapsed ms")
            timeElapsed -= time
        }
    }

    /**
     * Called when a click occurred. Can be tracked by listening for [ClickSlotC2SPacket]
     */
    @JvmStatic
    fun clickOccurred() {
        clickChronometer.reset()
    }

    /**
     * Called when the inventory was opened. Can be tracked by listening for [OpenScreenS2CPacket]
     */
    @JvmStatic
    fun inventoryOpened() {
        openSinceChronometer.reset()
        clickChronometer.reset()
    }

    /**
     * Listener for packets that are related to the inventory
     * to keep track of the inventory state and timings
     */
    val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val packet = event.packet

        // If we actually send a click packet, we can reset the click chronometer
        if (packet is ClickSlotC2SPacket && !event.isCancelled) {
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

    val screenHandler = handler<ScreenEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val screen = event.screen

        if (!event.isCancelled) {
            return@handler
        }

        if (screen is InventoryScreen || screen is GenericContainerScreen) {
            inventoryOpened()

            if (screen is InventoryScreen) {
                isInventoryOpenServerSide = true
            }
        }
    }

    val handleWorldChange = handler<WorldChangeEvent> {
        isInventoryOpenServerSide = false
    }

}

data class InventoryAction(
    val inventoryConstraints: InventoryConstraints,
    val screen: GenericContainerScreen? = null,
    var priority: Priority = Priority.NORMAL,
    val slot: ItemSlot,
    val button: Int,
    val actionType: SlotActionType,
) {

    companion object {

        fun click(constraints: InventoryConstraints,
                  screen: GenericContainerScreen? = null,
                  slot: ItemSlot,
                  button: Int,
                  actionType: SlotActionType) = InventoryAction(
            constraints,
            screen,
            slot = slot,
            button = button,
            actionType = actionType
        )

        fun performThrow(
            constraints: InventoryConstraints,
            screen: GenericContainerScreen? = null,
            slot: ItemSlot
        ) = InventoryAction(
            constraints,
            screen,
            slot = slot,
            button = 1,
            actionType = SlotActionType.THROW
        )

        fun performQuickMove(
            constraints: InventoryConstraints,
            screen: GenericContainerScreen? = null,
            slot: ItemSlot
        ) = InventoryAction(
            constraints,
            screen,
            slot = slot,
            button = 0,
            actionType = SlotActionType.QUICK_MOVE
        )

        fun performSwap(
            constraints: InventoryConstraints,
            screen: GenericContainerScreen? = null,
            from: ItemSlot,
            to: HotbarItemSlot
        ) = InventoryAction(
            constraints,
            screen,
            slot = from,
            button = to.hotbarSlotForServer,
            actionType = SlotActionType.SWAP
        )

    }

    fun priority(priority: Priority) = apply { this.priority = priority }

    fun canPerformAction(): Boolean {
        // Check constrains
        if (!inventoryConstraints.passesRequirements) {
            return false
        }

        // Screen is null, which means we are targeting the player inventory
        if (screen == null && player.currentScreenHandler.isPlayerInventory) {
            return true
        }

        // Check if current screen is the same as the screen we want to interact with
        val screen = mc.currentScreen as? GenericContainerScreen ?: return false
        return screen.syncId == this.screen.syncId
    }

    fun performAction(): Boolean {
        val slotId = slot.getIdForServer(screen) ?: return false
        interaction.clickSlot(screen?.syncId ?: 0, slotId, button, actionType, player)

        return true
    }

}
