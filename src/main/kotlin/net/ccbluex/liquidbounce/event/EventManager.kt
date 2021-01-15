/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import java.util.*

/**
 * A modern and fast event handler using lambda handlers
 */
object EventManager {

    private val registry = HashMap<Class<out Event>, MutableList<EventHook<in Event>>>()

    /**
     * Registers an event hook for events of type [T]
     */
    private inline fun <reified T : Event> handler(
        listener: Listenable,
        ignoreCondition: Boolean = false,
        noinline eventHandler: (T) -> Unit
    ) {
        registerEventHook(T::class.java, EventHook(listener, eventHandler, ignoreCondition))
    }

    /**
     * Used by [handler]
     */
    fun <T: Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        registry.computeIfAbsent(eventClass) { mutableListOf() }.add(eventHook as EventHook<in Event>)
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
    fun callEvent(event: Event) {
        val target = registry[event.javaClass] ?: return

        for (eventHook in target) {
            if (!eventHook.ignoresCondition && !eventHook.handlerClass.handleEvents())
                continue

            eventHook.handler(event)
        }
    }

}
