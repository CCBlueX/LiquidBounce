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

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * Contains all classes of events. Used to create lookup tables ahead of time
 */
val ALL_EVENT_CLASSES: Array<KClass<out Event>> = arrayOf(
    GameTickEvent::class,
    GameRenderTaskQueueEvent::class,
    BlockChangeEvent::class,
    ChunkLoadEvent::class,
    ChunkDeltaUpdateEvent::class,
    ChunkUnloadEvent::class,
    DisconnectEvent::class,
    GameRenderEvent::class,
    WorldRenderEvent::class,
    OverlayRenderEvent::class,
    ScreenRenderEvent::class,
    WindowResizeEvent::class,
    FrameBufferResizeEvent::class,
    MouseButtonEvent::class,
    MouseScrollEvent::class,
    MouseCursorEvent::class,
    KeyboardKeyEvent::class,
    KeyboardCharEvent::class,
    InputHandleEvent::class,
    MovementInputEvent::class,
    SprintEvent::class,
    KeyEvent::class,
    MouseRotationEvent::class,
    KeybindChangeEvent::class,
    KeybindIsPressedEvent::class,
    AttackEntityEvent::class,
    SessionEvent::class,
    ScreenEvent::class,
    ChatSendEvent::class,
    ChatReceiveEvent::class,
    UseCooldownEvent::class,
    BlockShapeEvent::class,
    BlockBreakingProgressEvent::class,
    BlockVelocityMultiplierEvent::class,
    BlockSlipperinessMultiplierEvent::class,
    EntityMarginEvent::class,
    HealthUpdateEvent::class,
    DeathEvent::class,
    PlayerTickEvent::class,
    PlayerPostTickEvent::class,
    PlayerMovementTickEvent::class,
    PlayerNetworkMovementTickEvent::class,
    PlayerPushOutEvent::class,
    PlayerMoveEvent::class,
    PlayerJumpEvent::class,
    PlayerAfterJumpEvent::class,
    PlayerUseMultiplier::class,
    PlayerInteractedItem::class,
    PlayerVelocityStrafe::class,
    PlayerStrideEvent::class,
    PlayerSafeWalkEvent::class,
    CancelBlockBreakingEvent::class,
    PlayerStepEvent::class,
    PlayerStepSuccessEvent::class,
    FluidPushEvent::class,
    PipelineEvent::class,
    PacketEvent::class,
    ClientStartEvent::class,
    ClientShutdownEvent::class,
    ClientLanguageChangedEvent::class,
    ValueChangedEvent::class,
    ModuleActivationEvent::class,
    ModuleToggleEvent::class,
    NotificationEvent::class,
    ClientChatStateChange::class,
    ClientChatMessageEvent::class,
    ClientChatErrorEvent::class,
    ClientChatJwtTokenEvent::class,
    WorldChangeEvent::class,
    AccountManagerMessageEvent::class,
    AccountManagerAdditionResultEvent::class,
    AccountManagerLoginResultEvent::class,
    VirtualScreenEvent::class,
    FpsChangeEvent::class,
    ClientPlayerDataEvent::class,
    RotationUpdateEvent::class,
    SplashOverlayEvent::class,
    SplashProgressEvent::class,
    RefreshArrayListEvent::class,
    BrowserReadyEvent::class,
    ServerConnectEvent::class,
    ServerPingedEvent::class,
    TargetChangeEvent::class,
    BlockCountChangeEvent::class,
    GameModeChangeEvent::class,
    ComponentsUpdate::class,
    ResourceReloadEvent::class,
    ProxyAdditionResultEvent::class,
    ProxyEditResultEvent::class,
    ProxyCheckResultEvent::class,
    ScaleFactorChangeEvent::class,
    DrawOutlinesEvent::class,
    OverlayMessageEvent::class,
    ScheduleInventoryActionEvent::class,
    SelectHotbarSlotSilentlyEvent::class,
    SpaceSeperatedNamesChangeEvent::class,
    ClickGuiScaleChangeEvent::class,
    BrowserUrlChangeEvent::class,
    TagEntityEvent::class,
    MouseScrollInHotbarEvent::class,
    PlayerFluidCollisionCheckEvent::class,
    PlayerSneakMultiplier::class,
    PerspectiveEvent::class,
    ItemLoreQueryEvent::class,
    PlayerEquipmentChangeEvent::class,
    ClickGuiValueChangeEvent::class,
    BlockAttackEvent::class,
    QueuePacketEvent::class,
    MinecraftAutoJumpEvent::class
)

/**
 * A modern and fast event handler using lambda handlers
 */
object EventManager {

    private val registry: Map<Class<out Event>, CopyOnWriteArrayList<EventHook<in Event>>> =
        ALL_EVENT_CLASSES.associate { Pair(it.java, CopyOnWriteArrayList()) }

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

        for (eventHook in target) {
            if (!eventHook.handlerClass.running) {
                continue
            }

            runCatching {
                eventHook.handler(event)
            }.onFailure {
                logger.error("Exception while executing handler.", it)
            }
        }

        return event
    }
}
