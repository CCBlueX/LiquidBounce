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
package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Contains all classes of events. Used to create lookup tables ahead of time
 */
@JvmField
internal val ALL_EVENT_CLASSES: Array<Class<out Event>> = arrayOf(
    GameTickEvent::class.java,
    GameRenderTaskQueueEvent::class.java,
    TickPacketProcessEvent::class.java,
    BlockChangeEvent::class.java,
    ChunkLoadEvent::class.java,
    ChunkDeltaUpdateEvent::class.java,
    ChunkUnloadEvent::class.java,
    DisconnectEvent::class.java,
    GameRenderEvent::class.java,
    WorldRenderEvent::class.java,
    OverlayRenderEvent::class.java,
    ScreenRenderEvent::class.java,
    WindowResizeEvent::class.java,
    FrameBufferResizeEvent::class.java,
    MouseButtonEvent::class.java,
    MouseScrollEvent::class.java,
    MouseCursorEvent::class.java,
    KeyboardKeyEvent::class.java,
    KeyboardCharEvent::class.java,
    InputHandleEvent::class.java,
    MovementInputEvent::class.java,
    SprintEvent::class.java,
    SneakNetworkEvent::class.java,
    KeyEvent::class.java,
    MouseRotationEvent::class.java,
    KeybindChangeEvent::class.java,
    KeybindIsPressedEvent::class.java,
    AttackEntityEvent::class.java,
    SessionEvent::class.java,
    ScreenEvent::class.java,
    ChatSendEvent::class.java,
    ChatReceiveEvent::class.java,
    UseCooldownEvent::class.java,
    BlockShapeEvent::class.java,
    BlockBreakingProgressEvent::class.java,
    BlockVelocityMultiplierEvent::class.java,
    BlockSlipperinessMultiplierEvent::class.java,
    EntityMarginEvent::class.java,
    EntityHealthUpdateEvent::class.java,
    HealthUpdateEvent::class.java,
    DeathEvent::class.java,
    PlayerTickEvent::class.java,
    PlayerPostTickEvent::class.java,
    PlayerMovementTickEvent::class.java,
    PlayerNetworkMovementTickEvent::class.java,
    PlayerPushOutEvent::class.java,
    PlayerMoveEvent::class.java,
    PlayerJumpEvent::class.java,
    PlayerAfterJumpEvent::class.java,
    PlayerUseMultiplier::class.java,
    PlayerInteractItemEvent::class.java,
    PlayerInteractedItemEvent::class.java,
    ClientPlayerInventoryEvent::class.java,
    PlayerVelocityStrafe::class.java,
    PlayerStrideEvent::class.java,
    PlayerSafeWalkEvent::class.java,
    CancelBlockBreakingEvent::class.java,
    PlayerStepEvent::class.java,
    PlayerStepSuccessEvent::class.java,
    FluidPushEvent::class.java,
    PipelineEvent::class.java,
    PacketEvent::class.java,
    ClientStartEvent::class.java,
    ClientShutdownEvent::class.java,
    ClientLanguageChangedEvent::class.java,
    ValueChangedEvent::class.java,
    ModuleActivationEvent::class.java,
    ModuleToggleEvent::class.java,
    NotificationEvent::class.java,
    ClientChatStateChange::class.java,
    ClientChatMessageEvent::class.java,
    ClientChatErrorEvent::class.java,
    ClientChatJwtTokenEvent::class.java,
    WorldChangeEvent::class.java,
    AccountManagerMessageEvent::class.java,
    AccountManagerAdditionResultEvent::class.java,
    AccountManagerRemovalResultEvent::class.java,
    AccountManagerLoginResultEvent::class.java,
    VirtualScreenEvent::class.java,
    FpsChangeEvent::class.java,
    FpsLimitEvent::class.java,
    ClientPlayerDataEvent::class.java,
    RotationUpdateEvent::class.java,
    RefreshArrayListEvent::class.java,
    BrowserReadyEvent::class.java,
    ServerConnectEvent::class.java,
    ServerPingedEvent::class.java,
    TargetChangeEvent::class.java,
    BlockCountChangeEvent::class.java,
    GameModeChangeEvent::class.java,
    ComponentsUpdate::class.java,
    ResourceReloadEvent::class.java,
    ProxyCheckResultEvent::class.java,
    ScaleFactorChangeEvent::class.java,
    DrawOutlinesEvent::class.java,
    OverlayMessageEvent::class.java,
    ScheduleInventoryActionEvent::class.java,
    SelectHotbarSlotSilentlyEvent::class.java,
    SpaceSeperatedNamesChangeEvent::class.java,
    ClickGuiScaleChangeEvent::class.java,
    BrowserUrlChangeEvent::class.java,
    TagEntityEvent::class.java,
    MouseScrollInHotbarEvent::class.java,
    PlayerFluidCollisionCheckEvent::class.java,
    PlayerSneakMultiplier::class.java,
    PerspectiveEvent::class.java,
    ItemLoreQueryEvent::class.java,
    PlayerEquipmentChangeEvent::class.java,
    ClickGuiValueChangeEvent::class.java,
    BlockAttackEvent::class.java,
    QueuePacketEvent::class.java,
    MinecraftAutoJumpEvent::class.java,
    WorldEntityRemoveEvent::class.java,
    TitleEvent.Title::class.java,
    TitleEvent.Subtitle::class.java,
    TitleEvent.Fade::class.java,
    TitleEvent.Clear::class.java,
)

/**
 * A modern and fast event handler using lambda handlers
 */
object EventManager {

    private val registry: Map<Class<out Event>, CopyOnWriteArrayList<EventHook<in Event>>> =
        ALL_EVENT_CLASSES.associateWithTo(
            Reference2ObjectOpenHashMap(ALL_EVENT_CLASSES.size)
        ) { CopyOnWriteArrayList() }

    init {
        SequenceManager
    }

    /**
     * Used by handler methods
     */
    fun <T : Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>): EventHook<T> {
        val handlers = registry[eventClass]
            ?: error("The event '${eventClass.name}' is not registered in Events.kt::ALL_EVENT_CLASSES.")

        @Suppress("UNCHECKED_CAST")
        val hook = eventHook as EventHook<in Event>

        if (!handlers.contains(hook)) {
            // `handlers` is sorted descending by EventHook.priority
            handlers.sortedInsert(hook) { -it.priority }
        }

        return eventHook
    }

    /**
     * Unregisters a handler.
     */
    fun <T : Event> unregisterEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        @Suppress("UNCHECKED_CAST")
        registry[eventClass]?.remove(eventHook as EventHook<in Event>)
    }

    fun unregisterEventHandler(eventListener: EventListener) {
        registry.values.forEach {
            it.removeIf { it.handlerClass == eventListener }
        }
    }

    fun unregisterAll() {
        registry.values.forEach {
            it.clear()
        }
    }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun <T : Event> callEvent(event: T): T {
        if (isDestructed) {
            return event
        }

        val target = registry[event.javaClass] ?: return event

        event.isCompleted = false
        for (eventHook in target) {
            if (!eventHook.handlerClass.running) {
                continue
            }

            runCatching {
                eventHook.handler.accept(event)
            }.onFailure {
                logger.error("Exception while executing handler.", it)
            }
        }
        event.isCompleted = true

        return event
    }
}
