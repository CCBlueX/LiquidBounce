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

import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * A modern and fast event handler using lambda handlers.
 */
object EventManager {

    private val registry = EventTypeRegistry()

    internal val allEventClasses: Array<out Class<out Event>>
        get() = EventTypeRegistry.ALL_EVENT_CLASSES

    init {
        CoroutineTicker
    }

    /**
     * Used by handler methods
     */
    fun <T : Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>): EventHook<T> {
        @Suppress("UNCHECKED_CAST")
        registry.getEventHooks(eventClass as Class<T>)?.addIfAbsent(eventHook)
            ?: error("The event '${eventClass.name}' is not registered in Events.kt::ALL_EVENT_CLASSES.")

        return eventHook
    }

    /**
     * Unregisters a handler.
     */
    fun <T : Event> unregisterEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        @Suppress("UNCHECKED_CAST")
        registry.getEventHooks(eventClass as Class<T>)?.remove(eventHook)
    }

    fun unregisterEventHandler(eventListener: EventListener) {
        registry.allEventHooks.forEach {
            it.remove(eventListener)
        }
    }

    fun unregisterAll() {
        registry.allEventHooks.forEach {
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

        val target = registry.getEventHooks(event) ?: return event

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
