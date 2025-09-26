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
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer.httpServer
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.util.Util
import org.apache.commons.io.output.StringBuilderWriter

internal object SocketEventListener : EventListener {

    private val events = ALL_EVENT_CLASSES
        .filter { WebSocketEvent::class.java.isAssignableFrom(it) }
        .associateBy { it.eventName }

    /**
     * Contains all events that are registered in the current context
     */
    private val registeredEvents = Reference2ObjectOpenHashMap<Class<out Event>, EventHook<in Event>>()

    private val writeBuffer = ThreadLocal.withInitial { StringBuilderWriter(DEFAULT_BUFFER_SIZE) }

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
        EventManager.registerEventHook(eventClass, eventHook)
    }

    fun unregister(name: String) {
        val eventClass = events[name] ?:
            throw IllegalArgumentException("Unknown event: $name")
        val eventHook = registeredEvents[eventClass] ?:
            throw IllegalArgumentException("No EventHook for event: $eventClass")

        EventManager.unregisterEventHook(eventClass, eventHook)
    }

    private fun writeToSockets(event: Event) = Util.getMainWorkerExecutor().execute {
        val json = writeBuffer.get().runCatching {
            JsonWriter(this).use { writer ->
                writer.beginObject()
                writer.name("name").value(event.javaClass.eventName)
                writer.name("event")
                (event as WebSocketEvent).serializer.toJson(event, event.javaClass, writer)
                writer.endObject()
            }
            toString().also { builder.clear() }
        }.onFailure {
            logger.error("Failed to serialize event $event", it)
        }.getOrNull() ?: return@execute

        httpServer.webSocketController.broadcast(json) { _, t ->
            logger.error("WebSocket event broadcast failed, JSON: $json", t)
        }
    }



}
