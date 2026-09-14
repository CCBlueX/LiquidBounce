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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import java.util.function.Consumer

/**
 * Event subscription without the Kotlin-only `handler<T> {}` DSL, so Java add-ons can listen too.
 */
object Events {

    /**
     * Calls [handler] for every [type] event while [owner] is running. Higher [priority] runs first.
     * Close the result to stop listening; unregistering [owner] does the same.
     */
    @JvmStatic
    @JvmOverloads
    fun <E : Event> subscribe(
        owner: EventListener,
        type: Class<E>,
        priority: Short = 0,
        handler: Consumer<E>,
    ): AutoCloseable {
        val hook = EventManager.registerEventHook(type, EventHook(owner, priority, handler))
        return AutoCloseable { EventManager.unregisterEventHook(type, hook) }
    }

    @JvmStatic
    fun <E : Event> post(event: E): E = EventManager.callEvent(event)

}
