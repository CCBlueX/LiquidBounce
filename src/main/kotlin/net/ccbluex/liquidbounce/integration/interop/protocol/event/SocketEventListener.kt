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
 */
package net.ccbluex.liquidbounce.integration.interop.protocol.event

import com.google.gson.stream.JsonWriter
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer.httpServer
import net.ccbluex.liquidbounce.utils.client.logger
import org.apache.commons.io.output.StringBuilderWriter
import kotlin.reflect.KClass

/**
 * Empty event:
 * `{"name":"","event":{}}`
 */
private const val EVENT_JSON_BYTE_COUNT = 64

class SocketEventListener : EventListener {

    private val events = ALL_EVENT_CLASSES
        .filter { WebSocketEvent::class.java.isAssignableFrom(it.java) }
        .associateBy { it.eventName }

    /**
     * Contains all events that are registered in the current context
     */
    private val registeredEvents = hashMapOf<KClass<out Event>, EventHook<in Event>>()

    fun registerAll() {
        events.keys.forEach { register(it) }
    }

    fun register(name: String) {
        val eventClass = events[name] ?:
            throw IllegalArgumentException("Unknown event: $name")

        if (registeredEvents.containsKey(eventClass)) {
            error("Event $name is already registered")
        }

        val eventHook = EventHook(this, handler = ::writeToSockets)

        registeredEvents[eventClass] = eventHook
        EventManager.registerEventHook(eventClass.java, eventHook)
    }

    fun unregister(name: String) {
        val eventClass = events[name] ?:
            throw IllegalArgumentException("Unknown event: $name")
        val eventHook = registeredEvents[eventClass] ?:
            throw IllegalArgumentException("No EventHook for event: $eventClass")

        EventManager.unregisterEventHook(eventClass.java, eventHook)
    }

    private fun writeToSockets(event: Event) = withScope {
        val json = runCatching {
            StringBuilderWriter(EVENT_JSON_BYTE_COUNT).use {
                JsonWriter(it).use { writer ->
                    writer.beginObject()
                    writer.name("name").value(event::class.eventName)
                    writer.name("event")
                    (event as WebSocketEvent).serializer.toJson(event, event::class.java, writer)
                    writer.endObject()
                }
                it.toString()
            }
        }.onFailure {
            logger.error("Failed to serialize event $event", it)
        }.getOrNull() ?: return@withScope

        httpServer.webSocketController.broadcast(json) { _, t ->
            logger.error("WebSocket event broadcast failed", t)
        }
    }



}
