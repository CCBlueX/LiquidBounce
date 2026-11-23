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

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.additions.setPosition
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventListenerNotListeningException
import net.ccbluex.liquidbounce.event.Guards
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.global.GlobalSettingsInventory
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_11_1
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.network.send1_11_1OpenInventory
import net.ccbluex.liquidbounce.utils.network.sendCloseInventory
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
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
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import org.joml.Vector2f
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.TimeSource

/**
 * Manages the inventory state and timings and schedules inventory actions
 *
 * TODO:
 *  - Progress Bar
 *  - Off-screen actions
 */
@Suppress("TooManyFunctions")
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
            if (!value) {
                cursor = null
            }
            field = value
        }

    var lastClickedSlot: Int = -1
        internal set

    private var recentInventoryOpen = false

    /**
     * Waits [millis] and continues right then, between two ticks. A wait that continues on a tick would round every
     * click delay up to the next one.
     */
    private suspend fun pause(millis: Int) {
        if (millis > 0) {
            withContext(Dispatchers.Minecraft) { delay(millis.toLong()) }
        }
    }

    /**
     * Where the cursor is taken to be while no screen shows it, in pixels of the menu. Null in the middle, where
     * opening puts it.
     */
    private var cursor: Vector2f? = null

    private fun AbstractContainerMenu.center() = Vector2f(
        (slots.minOf { it.x } + slots.maxOf { it.x }) / 2f + 8f,
        (slots.minOf { it.y } + slots.maxOf { it.y }) / 2f + 8f,
    )

    /**
     * [items] of the player inventory in the order that takes the cursor the shortest way from each to the next.
     */
    fun <T> nearestFirst(items: List<T>, slot: (T) -> ItemSlot): List<T> {
        val menu = player.inventoryMenu
        fun position(item: T) = menu.slots.getOrNull(slot(item).getIdForServer(null) ?: -1)
            ?.let { Vector2f(it.x.toFloat(), it.y.toFloat()) }

        val left = items.toMutableList()
        var from = cursor ?: menu.center()
        return List(items.size) {
            val next = left.minBy { position(it)?.distanceSquared(from) ?: Float.MAX_VALUE }
            left -= next
            position(next)?.let { from = it }
            next
        }
    }

    /**
     * Takes the cursor to the slot [action] clicks, which makes the delay before it longer the further that is.
     */
    private suspend fun moveCursor(action: InventoryAction, constraints: InventoryConstraints) {
        if (action !is InventoryAction.Click) {
            return
        }

        val menu = action.screen?.menu ?: player.inventoryMenu
        val slot = menu.slots.getOrNull(action.slot.getIdForServer(action.screen) ?: return) ?: return
        // A silent screen is not shown, so its cursor is the player's own
        val screen = (mc.gui.screen() as? AbstractContainerScreen<*>)?.takeIf {
            it.menu === menu && ModuleBetterInventory.shouldMoveCursor && !mc.mouseHandler.isMouseGrabbed &&
                !FeatureSilentScreen.shouldHide
        }
        val scale = mc.window.screenWidth.toFloat() / mc.window.guiScaledWidth
        // Nothing was clicked since opening
        val delay = if (cursor == null) constraints.startDelay else constraints.clickDelay

        val from = if (screen != null) {
            Vector2f(
                mc.mouseHandler.xpos().toFloat() / scale - screen.leftPos,
                mc.mouseHandler.ypos().toFloat() / scale - screen.topPos,
            )
        } else {
            cursor ?: menu.center()
        }
        // Somewhere on the slot, nobody hits the middle every time
        val to = Vector2f(slot.x + 2f + Random.nextFloat() * 12f, slot.y + 2f + Random.nextFloat() * 12f)
        // Slots are 18 pixels apart
        val millis = constraints.travelMillis(delay, from.distance(to) / 18f)
        cursor = to

        if (screen == null) {
            pause(millis)
            return
        }

        val start = TimeSource.Monotonic.markNow()
        do {
            val progress = if (millis > 0) {
                (start.elapsedNow().inWholeMilliseconds.toFloat() / millis).coerceAtMost(1f)
            } else {
                1f
            }
            // Slow at both ends
            val position = from.lerp(to, progress * progress * (3f - 2f * progress), Vector2f())
            mc.mouseHandler.setPosition(
                ((screen.leftPos + position.x) * scale).toDouble(),
                ((screen.topPos + position.y) * scale).toDouble(),
            )
            if (progress < 1f) {
                // Short enough for every frame to show a new position
                pause(5)
            }
        } while (progress < 1f && mc.gui.screen() === screen)
    }

    private class Request(
        val plan: InventoryPlan,
        val chain: InventoryAction.Chain,
        val guards: Guards?,
        val continuation: CancellableContinuation<Unit>,
    )

    /**
     * Chains the running plans wait for. Only touched on the render thread.
     */
    private val requests = ArrayList<Request>()

    internal fun request(
        plan: InventoryPlan,
        chain: InventoryAction.Chain,
        guards: Guards?,
        continuation: CancellableContinuation<Unit>,
    ) {
        requests += Request(plan, chain, guards, continuation)
    }

    /**
     * A wait for the server that ends by itself after [GlobalSettingsInventory.responseTime].
     */
    private class ServerWait {
        @Volatile
        private var job: Job? = null

        val isPending
            get() = job?.isActive == true

        /**
         * @param onTimeout runs on the render thread when the time passes without [stop]
         */
        fun start(onTimeout: () -> Unit = {}) {
            job?.cancel()
            job = InventoryManager.eventListenerScope.launch {
                delay(GlobalSettingsInventory.responseTime.toLong())
                onTimeout()
            }
        }

        fun stop() {
            job?.cancel()
        }
    }

    /**
     * After interacting with a container block or an entity, the server may already have a container open
     * that we do not know of yet. It silently drops clicks for another container id, so player inventory
     * clicks sent in that window are only applied client-side.
     */
    private val containerOpenWait = ServerWait()

    val isContainerOpenPending
        get() = containerOpenWait.isPending

    /**
     * Pause after hotbar [InventoryAction.UseItem] so silent slot reset and equip finish before clicking.
     */
    private val useItemWait = ServerWait()

    val isUseItemPending
        get() = useItemWait.isPending

    private class SentClick(
        val containerId: Int,
        val slotId: Int,
        val actionType: ContainerInput,
        val stack: ItemStack,
        val wasMoving: Boolean,
    ) {
        /**
         * Until the server answered or will not anymore, then until it had the time to take a restore back.
         */
        val wait = ServerWait()

        /**
         * Whether a resync put the item back.
         */
        var isRestored = false

        companion {
            /**
             * What clicking as [action] says sends, unless its slot is empty or not part of its screen.
             */
            fun of(action: InventoryAction): SentClick? {
                if (action !is InventoryAction.Click || action.slot.itemStack.isEmpty) {
                    return null
                }

                return SentClick(
                    action.screen.syncId,
                    action.slot.getIdForServer(action.screen) ?: return null,
                    action.actionType,
                    action.slot.itemStack.copy(),
                    wasMoving = !InventoryRequirements.NO_MOVEMENT.test(action),
                )
            }
        }
    }

    private class Rejection(val stack: ItemStack, var count: Int = 0)

    private val recentClicks = ArrayList<SentClick>()
    private val rejections = ArrayList<Rejection>()

    /**
     * Pause after a click the server rejected, so the same click is not sent right again.
     */
    private val revertWait = ServerWait()

    /**
     * A resync put a clicked item back and it is open whether the server rejected the click or only answered
     * an earlier one, or it did reject one. Clicking on would act on an inventory that is about to change again.
     */
    val isRevertPending
        get() = revertWait.isPending || recentClicks.any { it.isRestored }

    val canClickPlayerInventory
        get() = player.containerMenu.isPlayerInventory &&
            !interaction.isServerControlledInventory &&
            !isContainerOpenPending &&
            !isUseItemPending &&
            !isRevertPending

    /**
     * Items the server keeps putting back, such as lobby menu items. Moving them only loops.
     */
    private val serverLockedItems = ArrayList<ItemStack>()

    val hasServerLockedItems: Boolean
        get() = serverLockedItems.isNotEmpty()

    internal val serverLockedCount: Int
        get() = serverLockedItems.size

    fun isServerLocked(stack: ItemStack): Boolean =
        !stack.isEmpty && serverLockedItems.any { ItemStack.isSameItemSameComponents(it, stack) }

    @Suppress("unused")
    private val executor = tickHandler {
        if (!inGame) {
            endRequests { CancellationException("Not in game") }
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
        var isDone = false

        while (!isDone) {
            debugParameter("Requests") { requests.size }

            val request = takeRequest()
            when {
                request != null -> try {
                    maximumCloseDelay = max(maximumCloseDelay, perform(request))
                } finally {
                    // Its plan would wait forever if the chain did not get to its end
                    request.continuation.cancel()
                }

                !isInventoryOpen || !canCloseMainInventory -> isDone = true

                // A plan may ask for its next chain during the wait
                maximumCloseDelay > 0 -> {
                    pause(maximumCloseDelay)
                    maximumCloseDelay = 0
                }

                else -> {
                    network.sendCloseInventory()
                    isDone = true
                }
            }
        }

        lastClickedSlot = -1
    }

    /**
     * Ends the plans waiting for a chain for which [cause] returns why, at the suspension they wait in.
     */
    private inline fun endRequests(cause: (Request) -> Throwable?) {
        val ended = ArrayList<Pair<Request, Throwable>>()
        for (request in requests) {
            cause(request)?.let { ended += request to it }
        }

        // Resuming runs plan code, which must not find the list half changed
        for ((request, _) in ended) {
            requests -= request
        }
        for ((request, throwable) in ended) {
            request.continuation.resumeWithException(throwable)
        }
    }

    /**
     * The chain to perform next: possible right now, not needing the player inventory before needing it, then by
     * priority, then the one waiting longest.
     */
    private fun takeRequest(): Request? {
        requests.removeIf { !it.continuation.isActive }
        endRequests { request ->
            val listener = request.plan.listener
            if (!listener.running) {
                EventListenerNotListeningException(listener)
            } else {
                try {
                    request.guards?.broken()
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    e
                }
            }
        }

        return requests
            .filter { it.chain.canPerformAction() }
            .minWithOrNull(compareBy(COMPARATOR_ACTION_CHAIN) { it.chain })
            ?.also { requests -= it }
    }

    /**
     * Performs the chain of [request] without interruption. Its plan continues right after the last action, while
     * the inventory is what that plan expects; the click delay passes before the next chain.
     *
     * @return the close delay of its constraints
     */
    @Suppress("CognitiveComplexMethod")
    private suspend fun perform(request: Request): Int {
        val chain = request.chain
        val constraints = chain.inventoryConstraints

        for ((index, action) in chain.actions.withIndex()) {
            // First action to be executed will be the start delay
            if (recentInventoryOpen) {
                recentInventoryOpen = false
                pause(constraints.startDelay.random())
            }

            // Handle player inventory open requirements
            if (action.requiresPlayerInventoryOpen()) {
                if (!isInventoryOpen) {
                    network.send1_11_1OpenInventory()
                    pause(constraints.startDelay.random())
                }
            } else if (canCloseMainInventory && isInventoryOpen) {
                pause(constraints.closeDelay.random())
                network.sendCloseInventory()
            }

            // Check if this is the first action in the chain, which allows us to simulate a miss click
            // This is only possible for container-type slots and also does not make much sense when
            // the action is a throw action (you cannot miss-click really when throwing)
            if (index == 0 && action is InventoryAction.Click
                && constraints.missChance.random() > Random.nextInt(100)
                && action.actionType != ContainerInput.THROW
                && chain.canPerformAction()
            ) {
                // Simulate a miss click (this is only possible for container-type slots)
                // TODO: Add support for inventory slots
                if (action.performMissClick()) {
                    pause(constraints.clickDelay.random())
                }
            }

            if (action is InventoryAction.CloseScreen) {
                pause(constraints.closeDelay.random())
            }

            moveCursor(action, constraints)

            // The waits may outlive the chain, e.g. when its menu closes meanwhile
            if (!chain.canPerformAction()) {
                logger.warn("Cannot perform action $action because it is not possible")
                request.continuation.cancel(CancellationException("The chain is no longer possible"))
                return 0
            }

            val clickRecord = SentClick.of(action)
            if (action.performAction()) {
                request.plan.onPerformed(action)
                if (clickRecord != null) {
                    recentClicks += clickRecord
                    // A click the server took as predicted is never answered.
                    clickRecord.wait.start { recentClicks -= clickRecord }
                }
                // A chain that goes on after a use says itself what follows it
                if (action is InventoryAction.UseItem && index == chain.actions.lastIndex) {
                    useItemWait.start()
                }
                if (index == chain.actions.lastIndex) {
                    request.continuation.resume(Unit)
                }
                if (action is InventoryAction.CloseScreen) {
                    waitTicks(1)
                } else {
                    pause(constraints.clickDelay.random())
                }
            }
        }

        if (request.continuation.isActive) {
            request.continuation.resume(Unit)
        }
        return constraints.closeDelay.random()
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

        if (packet is ServerboundContainerClickPacket) {
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
            containerOpenWait.start()
        } else if (packet is ClientboundOpenScreenPacket) {
            containerOpenWait.stop()
        }

        if (packet is ClientboundContainerSetContentPacket) {
            val containerId = packet.containerId
            val items = packet.items
            mc.execute { onContainerResync(containerId, items) }
        } else if (packet is ClientboundContainerSetSlotPacket && packet.containerId == 0) {
            val slot = packet.slot
            val stack = packet.item
            mc.execute { onSlotResync(slot, stack) }
        }

        // A closed container is never resynced again, so its clicks stay unjudged.
        if (packet is ServerboundContainerClosePacket || packet is ClientboundContainerClosePacket) {
            mc.execute { forgetClicks { it.containerId != 0 } }
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
        endRequests { CancellationException("World changed") }
        isInventoryOpenServerSide = false
        containerOpenWait.stop()
        useItemWait.stop()
        revertWait.stop()
        forgetClicks { true }
        rejections.clear()
        serverLockedItems.clear()
    }

    private inline fun forgetClicks(crossinline predicate: (SentClick) -> Boolean) {
        recentClicks.removeIf { click -> predicate(click).also { if (it) click.wait.stop() } }
    }

    private fun isRevertibleAction(actionType: ContainerInput): Boolean =
        actionType == ContainerInput.THROW ||
            actionType == ContainerInput.SWAP ||
            actionType == ContainerInput.QUICK_MOVE

    private fun recordRevert(click: SentClick) {
        val stack = click.stack
        revertWait.start()

        // Servers that reject clicks of a moving player reject any item, which says nothing about this one.
        if (!click.wasMoving && !isServerLocked(stack)) {
            val rejection = rejections.find { ItemStack.isSameItemSameComponents(it.stack, stack) }
                ?: Rejection(stack).also(rejections::add)

            if (++rejection.count >= GlobalSettingsInventory.lockAfterRejections) {
                serverLockedItems += stack
            }
        }
    }

    /**
     * Whether the resynced slot holds the clicked item again. The first time it does, the server gets the time
     * to take that back before the click counts as reverted: a server that resends the inventory after every
     * click may only have answered the click before.
     */
    private fun isRestored(click: SentClick, slotStack: ItemStack): Boolean {
        val isRestored = isRevertibleAction(click.actionType) &&
            ItemStack.isSameItemSameComponents(slotStack, click.stack) && slotStack.count >= click.stack.count
        if (isRestored && !click.isRestored) {
            click.isRestored = true
            click.wait.start {
                recentClicks -= click
                recordRevert(click)
            }
        }
        return isRestored
    }

    private fun onContainerResync(containerId: Int, items: List<ItemStack>) {
        forgetClicks { click ->
            click.containerId == containerId && !isRestored(click, items.getOrNull(click.slotId) ?: ItemStack.EMPTY)
        }
    }

    private fun onSlotResync(slot: Int, stack: ItemStack) {
        forgetClicks { click -> click.containerId == 0 && click.slotId == slot && !isRestored(click, stack) }
    }

    private val COMPARATOR_ACTION_CHAIN: Comparator<InventoryAction.Chain> =
        compareBy<InventoryAction.Chain> {
            it.requiresInventoryOpen()
        }.thenByDescending {
            it.priority
        }
}
