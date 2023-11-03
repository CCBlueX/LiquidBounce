/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.utils.client.EventScheduler
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.reflect.full.findAnnotation

/**
 * A modern and fast event handler using lambda handlers
 */
object EventManager {

    private val registry = mutableMapOf<Class<out Event>, ArrayList<EventHook<in Event>>>()

    val mappedEvents = arrayOf(
        GameTickEvent::class,
        BlockChangeEvent::class,
        ChunkLoadEvent::class,
        ChunkUnloadEvent::class,
        WorldDisconnectEvent::class,
        GameRenderEvent::class,
        WorldRenderEvent::class,
        EngineRenderEvent::class,
        OverlayRenderEvent::class,
        ScreenRenderEvent::class,
        WindowResizeEvent::class,
        WindowFocusEvent::class,
        MouseButtonEvent::class,
        MouseScrollEvent::class,
        MouseCursorEvent::class,
        KeyboardKeyEvent::class,
        KeyboardCharEvent::class,
        InputHandleEvent::class,
        MovementInputEvent::class,
        KeyEvent::class,
        MouseRotationEvent::class,
        AttackEvent::class,
        SessionEvent::class,
        ScreenEvent::class,
        ChatSendEvent::class,
        UseCooldownEvent::class,
        BlockShapeEvent::class,
        BlockBreakingProgressEvent::class,
        BlockVelocityMultiplierEvent::class,
        BlockSlipperinessMultiplierEvent::class,
        EntityMarginEvent::class,
        PlayerTickEvent::class,
        PlayerMovementTickEvent::class,
        PlayerNetworkMovementTickEvent::class,
        PlayerPushOutEvent::class,
        PlayerMoveEvent::class,
        PlayerJumpEvent::class,
        PlayerUseMultiplier::class,
        PlayerVelocityStrafe::class,
        PlayerStrideEvent::class,
        PlayerSafeWalkEvent::class,
        TickJumpEvent::class,
        CancelBlockBreakingEvent::class,
        PlayerStepEvent::class,
        FluidPushEvent::class,
        PacketEvent::class,
        ClientStartEvent::class,
        ClientShutdownEvent::class,
        ValueChangedEvent::class,
        ToggleModuleEvent::class,
        NotificationEvent::class,
        ClientChatMessageEvent::class,
        ClientChatErrorEvent::class
    ).map { Pair(it.findAnnotation<Nameable>()!!.name, it) }

    init {
        SequenceManager
    }

    /**
     * Used by handler methods
     */
    fun <T : Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        val handlers = registry.computeIfAbsent(eventClass) { ArrayList() }

        val hook = eventHook as EventHook<in Event>

        if (!handlers.contains(hook)) {
            handlers.add(hook)

            handlers.sortByDescending { it.priority }
        }
    }

    /**
     * Unregisters a handler.
     */
    fun <T : Event> unregisterEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        registry[eventClass]?.remove(eventHook as EventHook<in Event>)
    }

    /**
     * Unregister listener
     *
     * @param listenable for unregister
     */
    fun unregisterListener(listenable: Listenable) {
        for ((key, handlerList) in registry) {
            handlerList.removeIf { it.handlerClass == listenable }

            registry[key] = handlerList
        }
    }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun <T : Event> callEvent(event: T): T {
        val target = registry[event.javaClass] ?: return event

        for (eventHook in target) {
            EventScheduler.process(event)

            if (!eventHook.ignoresCondition && !eventHook.handlerClass.handleEvents()) {
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
