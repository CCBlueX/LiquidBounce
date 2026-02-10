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
package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.ccbluex.liquidbounce.event.events.AccountManagerAdditionResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerLoginResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerMessageEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerRemovalResultEvent
import net.ccbluex.liquidbounce.event.events.AllowAutoJumpEvent
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.BedStateChangeEvent
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.BlockAttackEvent
import net.ccbluex.liquidbounce.event.events.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.BlockCountChangeEvent
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.BlockSlipperinessMultiplierEvent
import net.ccbluex.liquidbounce.event.events.BlockVelocityMultiplierEvent
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.BrowserUrlChangeEvent
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.ChatSendEvent
import net.ccbluex.liquidbounce.event.events.ChunkDeltaUpdateEvent
import net.ccbluex.liquidbounce.event.events.ChunkLoadEvent
import net.ccbluex.liquidbounce.event.events.ChunkUnloadEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiScaleChangeEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiValueChangeEvent
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatJwtTokenEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.ClientLanguageChangedEvent
import net.ccbluex.liquidbounce.event.events.ClientPlayerDataEvent
import net.ccbluex.liquidbounce.event.events.ClientPlayerEffectEvent
import net.ccbluex.liquidbounce.event.events.ClientPlayerInventoryEvent
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.events.ComponentsUpdateEvent
import net.ccbluex.liquidbounce.event.events.DeathEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.EntityEquipmentChangeEvent
import net.ccbluex.liquidbounce.event.events.EntityHealthUpdateEvent
import net.ccbluex.liquidbounce.event.events.EntityMarginEvent
import net.ccbluex.liquidbounce.event.events.FluidPushEvent
import net.ccbluex.liquidbounce.event.events.FpsChangeEvent
import net.ccbluex.liquidbounce.event.events.FpsLimitEvent
import net.ccbluex.liquidbounce.event.events.FramebufferResizeEvent
import net.ccbluex.liquidbounce.event.events.GameModeChangeEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.GameRenderTaskQueueEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.HealthUpdateEvent
import net.ccbluex.liquidbounce.event.events.InputHandleEvent
import net.ccbluex.liquidbounce.event.events.ItemLoreQueryEvent
import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.events.KeybindChangeEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.KeyboardCharEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.ModuleActivationEvent
import net.ccbluex.liquidbounce.event.events.ModuleToggleEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.MouseCursorEvent
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.OverlayMessageEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.events.PipelineEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerFluidCollisionCheckEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractItemEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractedItemEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerPushOutEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerStepEvent
import net.ccbluex.liquidbounce.event.events.PlayerStepSuccessEvent
import net.ccbluex.liquidbounce.event.events.PlayerStrideEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.ProxyCheckResultEvent
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.ScaleFactorChangeEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent
import net.ccbluex.liquidbounce.event.events.SelectHotbarSlotSilentlyEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.events.ServerPingedEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.events.SpaceSeperatedNamesChangeEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.events.TargetChangeEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.events.UseCooldownEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.event.events.ValueChangedEvent
import net.ccbluex.liquidbounce.event.events.VirtualScreenEvent
import net.ccbluex.liquidbounce.event.events.WindowResizeEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldEntityRemoveEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException

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
    FramebufferResizeEvent::class.java,
    MouseButtonEvent::class.java,
    MouseScrollEvent::class.java,
    MouseCursorEvent::class.java,
    KeyboardKeyEvent::class.java,
    KeyboardCharEvent::class.java,
    InputHandleEvent::class.java,
    MovementInputEvent::class.java,
    SprintEvent::class.java,
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
    ClientPlayerEffectEvent::class.java,
    RotationUpdateEvent::class.java,
    RefreshArrayListEvent::class.java,
    BrowserReadyEvent::class.java,
    ServerConnectEvent::class.java,
    ServerPingedEvent::class.java,
    TargetChangeEvent::class.java,
    BlockCountChangeEvent::class.java,
    BedStateChangeEvent::class.java,
    GameModeChangeEvent::class.java,
    ComponentsUpdateEvent::class.java,
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
    EntityEquipmentChangeEvent::class.java,
    ClickGuiValueChangeEvent::class.java,
    BlockAttackEvent::class.java,
    BlinkPacketEvent::class.java,
    AllowAutoJumpEvent::class.java,
    WorldEntityRemoveEvent::class.java,
    TitleEvent.Title::class.java,
    TitleEvent.Subtitle::class.java,
    TitleEvent.Fade::class.java,
    TitleEvent.Clear::class.java,
    UserLoggedInEvent::class.java,
    UserLoggedOutEvent::class.java,
)

/**
 * A modern and fast event handler using lambda handlers
 */
object EventManager {

    private val registry: Map<Class<out Event>, EventHookRegistry<in Event>> =
        ALL_EVENT_CLASSES.associateWithTo(
            Reference2ObjectOpenHashMap(ALL_EVENT_CLASSES.size)
        ) { EventHookRegistry() }

    private val flows: Map<Class<out Event>, MutableSharedFlow<Event>> =
        ALL_EVENT_CLASSES.associateWithTo(
            Reference2ObjectOpenHashMap(ALL_EVENT_CLASSES.size)
        ) { MutableSharedFlow(replay = 0, extraBufferCapacity = 0) }

    /**
     * Used by handler methods
     */
    fun <T : Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>): EventHook<T> {
        val handlers = registry[eventClass]
            ?: error("The event '${eventClass.name}' is not registered in Events.kt::ALL_EVENT_CLASSES.")

        @Suppress("UNCHECKED_CAST")
        val hook = eventHook as EventHook<in Event>

        handlers.addIfAbsent(hook)

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
            it.remove(eventListener)
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

        val eventType = event.javaClass
        val target = registry[eventType] ?: return event

        event.isCompleted = false
        for (eventHook in target) {
            if (!eventHook.handlerClass.running) {
                continue
            }

            try {
                eventHook.handler.accept(event)
            } catch (e: ReportedException) {
                ErrorHandler.fatal(
                    error = e,
                    needToReport = true,
                    additionalMessage = "Event (${eventType.simpleName}) handler of ${eventHook.handlerClass}"
                )
            } catch (e: Throwable) {
                logger.error(
                    "Exception while executing event handler of {}, event={}",
                    eventHook.handlerClass.javaClass.simpleName,
                    event,
                    e,
                )
            }
        }
        event.isCompleted = true

        @Suppress("UNCHECKED_CAST")
        (flows[event.javaClass] as MutableSharedFlow<T>).tryEmit(event)

        return event
    }

    /**
     * Gets a [SharedFlow] for the given event class.
     * The flow receives the event instances after all [EventHook]s are executed.
     * So the [Event.isCompleted] will be true when the event is emitted.
     */
    fun <T : Event> flowOf(eventClass: Class<T>): SharedFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return flows[eventClass] as SharedFlow<T>
    }
}
