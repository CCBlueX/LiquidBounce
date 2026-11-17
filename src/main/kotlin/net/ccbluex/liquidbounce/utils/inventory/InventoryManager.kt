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
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_11_1
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.network.send1_11_1OpenInventory
import net.ccbluex.liquidbounce.utils.network.sendCloseInventory
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
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

    @AddonApi
    val isInventoryOpen
        get() = isInInventoryScreen || isInventoryOpenServerSide

    @AddonApi
    val isHandledScreenOpen
        get() = mc.gui.screen() is AbstractContainerScreen<*> || isInventoryOpenServerSide

    var isInventoryOpenServerSide = false
        set(value) {
            if (!field && value) {
                onInventoryOpened()
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
     * After interacting with a container block or an entity, the server may already have a container open
     * that we do not know of yet. It silently drops clicks for another container id, so player inventory
     * clicks sent in that window are only applied client-side.
     */
    private var containerOpenPendingUntil = 0L

    val isContainerOpenPending
        get() = System.currentTimeMillis() < containerOpenPendingUntil

    /**
     * Brief pause after hotbar [InventoryAction.UseItem] so silent slot reset and equip finish before clicking.
     */
    private var useItemCooldownUntil = 0L

    val isUseItemPending
        get() = System.currentTimeMillis() < useItemCooldownUntil

    private class SentClick(
        val containerId: Int,
        val slotId: Int,
        val actionType: ContainerInput,
        val stack: ItemStack,
        val time: Long,
    )

    private class Rejection(val stack: ItemStack, var count: Int = 0, var cooldownUntil: Long = 0L)

    private val recentClicks = ArrayDeque<SentClick>()
    private val rejections = ArrayList<Rejection>()
    private var consecutiveReverts = 0
    private var globalCooldownUntil = 0L

    val isGlobalRevertCooldown
        get() = System.currentTimeMillis() < globalCooldownUntil

    val canClickPlayerInventory
        get() = player.containerMenu.isPlayerInventory &&
            !interaction.isServerControlledInventory &&
            !isContainerOpenPending &&
            !isUseItemPending &&
            !isGlobalRevertCooldown

    /**
     * Items the server keeps putting back, such as lobby menu items. Moving them only loops.
     */
    private val serverLockedItems = ArrayList<ItemStack>()

    val hasBlockedItems: Boolean
        get() = serverLockedItems.isNotEmpty() || rejections.any { System.currentTimeMillis() < it.cooldownUntil }

    fun isServerLocked(stack: ItemStack): Boolean =
        !stack.isEmpty && serverLockedItems.any { ItemStack.isSameItemSameComponents(it, stack) }

    fun isItemBlocked(stack: ItemStack): Boolean {
        if (stack.isEmpty) {
            return false
        }
        if (isServerLocked(stack)) {
            return true
        }
        val now = System.currentTimeMillis()
        return rejections.any { now < it.cooldownUntil && ItemStack.isSameItemSameComponents(it.stack, stack) }
    }

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

        debugParameter("Inventory Open") { isInventoryOpen }
        debugParameter("Inventory Open Server Side") { isInventoryOpenServerSide }
        debugParameter("Cursor Stack") { player.containerMenu.carried }

        if (mc.gui.screen() == null && !player.containerMenu.carried.isEmpty && canClickPlayerInventory) {
            val emptySlot = Slots.Inventory.findSlot { it.isEmpty }
                ?: Slots.Hotbar.findSlot { it.isEmpty }
            if (emptySlot != null) {
                if (!isInventoryOpen) {
                    network.send1_11_1OpenInventory()
                    waitTicks(1)
                }
                if (InventoryAction.Click.performPickup(slot = emptySlot).performAction()) {
                    waitTicks(1)
                }
            }
        }

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

            debugParameter("Schedule Size") { schedule.size }

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
                            network.send1_11_1OpenInventory()
                            waitTicks(constraints.startDelay.random())
                            cycles = 0
                        }
                    } else if (canCloseMainInventory) {
                        // When all scheduled actions are done, we can close the inventory
                        if (isInventoryOpen) {
                            waitTicks(constraints.closeDelay.random())
                            cycles = 0
                            network.sendCloseInventory()
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
                        && action.actionType != ContainerInput.THROW
                    ) {
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
                    val clickRecord = if (action is InventoryAction.Click && !action.slot.itemStack.isEmpty) {
                        action.slot.getIdForServer(action.screen)?.let { slotId ->
                            SentClick(
                                action.screen.syncId,
                                slotId,
                                action.actionType,
                                action.slot.itemStack.copy(),
                                System.currentTimeMillis(),
                            )
                        }
                    } else {
                        null
                    }
                    if (action.performAction()) {
                        if (clickRecord != null) {
                            recentClicks.addLast(clickRecord)
                        }
                        when (action) {
                            is InventoryAction.CloseScreen -> {
                                waitTicks(1)
                                requiresUpdate = true
                                cycles = 0
                            }
                            is InventoryAction.UseItem -> {
                                useItemCooldownUntil = System.currentTimeMillis() + player.ping + USE_ITEM_GRACE_MS
                                waitTicks(max(constraints.clickDelay.random(), 2))
                                cycles = 0
                            }
                            else -> {
                                waitTicks(constraints.clickDelay.random())
                                cycles = 0
                            }
                        }
                    }
                }
            }
        } while (schedule.isNotEmpty())

        // When all scheduled actions are done, we can close the inventory
        if (isInventoryOpen && canCloseMainInventory) {
            waitTicks(maximumCloseDelay)
            network.sendCloseInventory()
        }

        lastClickedSlot = -1
    }

    /**
     * Called when a click occurs. Can be tracked by listening for [ServerboundContainerClickPacket]
     *
     * @see net.ccbluex.liquidbounce.injection.mixins.viaversion.MixinPacketWrapper
     */
    @JvmStatic
    fun onClickOccurs() {
        // Every click will require an update
        requiresUpdate = true
    }

    /**
     * Called when the inventory was opened. Can be tracked by listening for [ClientboundOpenScreenPacket]
     */
    @JvmStatic
    fun onInventoryOpened() {
        recentInventoryOpen = true
    }

    /**
     * Listener for packets that are related to the inventory
     * to keep track of the inventory state and timings
     */
    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val packet = event.packet

        if (!inGame || event.isCancelled) {
            return@handler
        }

        // If we actually send a click packet, we can reset the click chronometer
        if (packet is ServerboundContainerClickPacket) {
            onClickOccurs()

            if (packet.containerId == 0) {
                isInventoryOpenServerSide = true
            }
        }

        val mayOpenContainer = when (packet) {
            is ServerboundUseItemOnPacket -> {
                val pos = packet.hitResult.blockPos
                world.getBlockState(pos).getMenuProvider(world, pos) != null
            }
            is ServerboundInteractPacket -> true
            else -> false
        }

        if (mayOpenContainer) {
            containerOpenPendingUntil = System.currentTimeMillis() + player.ping + CONTAINER_OPEN_GRACE_MS
        } else if (packet is ClientboundOpenScreenPacket) {
            containerOpenPendingUntil = 0L
        }

        if (packet is ClientboundContainerSetContentPacket) {
            val containerId = packet.containerId
            val items = packet.items
            mc.execute { onContainerResync(containerId, items) }
        } else if (packet is ClientboundContainerSetSlotPacket && packet.containerId == 0 && !packet.item.isEmpty) {
            val slot = packet.slot
            val stack = packet.item
            mc.execute { onSlotResync(slot, stack) }
        }

        if (packet is ServerboundContainerClosePacket || packet is ClientboundContainerClosePacket
            || packet is ClientboundOpenScreenPacket
        ) {
            // Prevent closing inventory (no other screen!) if it is already closed
            if (!isInventoryOpenServerSide && packet is ServerboundContainerClosePacket && packet.containerId == 0) {
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

        debugParameter("Screen") { screen?.title }

        if (!inGame || event.isCancelled) {
            return@handler
        }

        if (screen is AbstractContainerScreen<*>) {
            debugParameter("Screen Handler Type") {
                screen.menu.typeOrNull?.let {
                    BuiltInRegistries.MENU.getKey(it)
                }
            }
            debugParameter("Screen Slot count") {
                val slots = screen.menu.slots
                "${slots.size} (${slots.count { it.container !== player.inventory }})"
            }
            // ViaFabricPlus injects into [tutorialManager.onInventoryOpened()] but we take
            // the easy way and just listen for the screen event.
            if (screen is InventoryScreen && isOlderThanOrEqual1_11_1) {
                isInventoryOpenServerSide = true
            }

            onInventoryOpened()
        }
    }

    @Suppress("unused")
    private val handleWorldChange = handler<WorldChangeEvent> {
        isInventoryOpenServerSide = false
        containerOpenPendingUntil = 0L
        useItemCooldownUntil = 0L
        consecutiveReverts = 0
        globalCooldownUntil = 0L
        recentClicks.clear()
        rejections.clear()
        serverLockedItems.clear()
    }

    private fun pruneRecentClicks(now: Long) {
        val window = player.ping + RESYNC_GRACE_MS
        while (recentClicks.firstOrNull()?.let { now - it.time > window } == true) {
            recentClicks.removeFirst()
            consecutiveReverts = 0
        }
    }

    private fun isRevertibleAction(actionType: ContainerInput): Boolean =
        actionType == ContainerInput.THROW ||
            actionType == ContainerInput.SWAP ||
            actionType == ContainerInput.QUICK_MOVE

    private fun recordRevert(stack: ItemStack, now: Long) {
        if (++consecutiveReverts >= GLOBAL_REVERT_THRESHOLD) {
            val shift = (consecutiveReverts - GLOBAL_REVERT_THRESHOLD).coerceAtMost(2)
            globalCooldownUntil = now + player.ping + (GLOBAL_REVERT_COOLDOWN_MS shl shift)
        }

        if (isServerLocked(stack)) {
            return
        }

        val rejection = rejections.find { ItemStack.isSameItemSameComponents(it.stack, stack) }
            ?: Rejection(stack).also(rejections::add)

        // Cooldown prevents re-sending the same SWAP before an in-flight click's response arrives.
        rejection.cooldownUntil = now + player.ping + RESYNC_COOLDOWN_MS
        if (++rejection.count >= LOCK_AFTER_REJECTIONS) {
            serverLockedItems += stack
        }
    }

    private fun onContainerResync(containerId: Int, items: List<ItemStack>) {
        val now = System.currentTimeMillis()
        pruneRecentClicks(now)

        val iterator = recentClicks.iterator()
        while (iterator.hasNext()) {
            val click = iterator.next()
            if (click.containerId != containerId) {
                continue
            }
            iterator.remove()

            if (!isRevertibleAction(click.actionType)) {
                continue
            }
            val slotStack = items.getOrNull(click.slotId) ?: continue
            if (ItemStack.isSameItemSameComponents(slotStack, click.stack) &&
                slotStack.count >= click.stack.count
            ) {
                recordRevert(click.stack, now)
            } else {
                consecutiveReverts = 0
            }
        }
    }

    private fun onSlotResync(slot: Int, stack: ItemStack) {
        val now = System.currentTimeMillis()
        pruneRecentClicks(now)

        val iterator = recentClicks.iterator()
        while (iterator.hasNext()) {
            val click = iterator.next()
            if (click.containerId != 0 || click.slotId != slot || !isRevertibleAction(click.actionType)) {
                continue
            }
            if (ItemStack.isSameItemSameComponents(stack, click.stack) && stack.count >= click.stack.count) {
                iterator.remove()
                recordRevert(click.stack, now)
                break
            }
        }
    }

    private val COMPARATOR_ACTION_CHAIN: Comparator<InventoryAction.Chain> =
        compareBy<InventoryAction.Chain> {
            it.requiresInventoryOpen()
        }.thenByDescending {
            it.priority
        }

    private const val CONTAINER_OPEN_GRACE_MS = 150L
    private const val USE_ITEM_GRACE_MS = 100L
    private const val RESYNC_GRACE_MS = 300L
    private const val RESYNC_COOLDOWN_MS = 200L
    private const val LOCK_AFTER_REJECTIONS = 3
    private const val GLOBAL_REVERT_THRESHOLD = 2
    private const val GLOBAL_REVERT_COOLDOWN_MS = 400L
}
