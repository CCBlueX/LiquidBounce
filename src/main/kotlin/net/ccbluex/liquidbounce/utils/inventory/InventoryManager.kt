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

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket
import net.minecraft.registry.Registries
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

    var lastClickedSlot: Int = -1
        internal set

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

        debugParameter(this, "Inventory Open", isInventoryOpen)
        debugParameter(this, "Inventory Open Server Side", isInventoryOpenServerSide)

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
            val schedule = event.schedule
                .filterTo(ObjectArrayList()) { actionChain ->
                    actionChain.canPerformAction() && actionChain.actions.isNotEmpty()
                }

            // If the schedule is empty, we can break the loop
            if (schedule.isEmpty) {
                break
            }

            // Schedule of actions that have to be executed
            // The schedule is sorted by
            // 1. With Non-inventory open required actions
            // 2. With inventory open required actions
            schedule.sortWith(COMPARATOR_ACTION_CHAIN)

            debugParameter(this, "Schedule Size", schedule.size)

            // Handle non-inventory open actions first
            for ((scheduleIndex, chained) in schedule.withIndex()) {
                // Do not continue if we need to update the schedule
                if (requiresUpdate) {
                    break
                }

                // These are chained actions that have to be executed in order
                // We cannot interrupt them
                debugParameter("Schedule Index") { scheduleIndex }
                debugParameter("Action Size") { chained.actions.size }
                for ((index, action) in chained.actions.withIndex()) {
                    debugParameter("Action Index") { index }
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
                    if (index == 0 && action is InventoryAction.Click
                        && constraints.missChance.random() > Random.nextInt(100)
                        && action.actionType != SlotActionType.THROW) {
                        // Simulate a miss click (this is only possible for container-type slots)
                        // TODO: Add support for inventory slots
                        if (action.performMissClick()) {
                            waitTicks(constraints.clickDelay.random())
                            cycles = 0
                        }
                    }

                    if (action is InventoryAction.CloseScreen) {
                        waitTicks(constraints.closeDelay.random())
                        cycles = 0
                    }
                    if (action.performAction()) {
                        if (action !is InventoryAction.CloseScreen) {
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

        lastClickedSlot = -1
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
    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
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

        debugParameter("Screen") { screen }

        if (event.isCancelled) {
            return@handler
        }

        if (screen is HandledScreen<*>) {
            debugParameter("Screen Handler Type") {
                val type = runCatching { screen.screenHandler.type }.getOrNull()
                type?.let {
                    Registries.SCREEN_HANDLER.getId(it)
                }
            }
            debugParameter("Screen Slot count") {
                val slots = screen.screenHandler.slots
                "${slots.size} (${slots.count { it.inventory !== player.inventory }})"
            }
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

    private val COMPARATOR_ACTION_CHAIN: Comparator<InventoryAction.Chain> =
        compareBy<InventoryAction.Chain> {
            it.requiresInventoryOpen()
        }.thenByDescending {
            it.priority
        }

}
