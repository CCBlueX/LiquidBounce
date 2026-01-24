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
package net.ccbluex.liquidbounce.integration.interop.protocol.event

import com.google.gson.stream.JsonWriter
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.ALL_EVENT_CLASSES
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.eventName
import net.ccbluex.liquidbounce.event.newEventHook
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

        val eventHook = newEventHook(
            priority = Short.MIN_VALUE, // Make sure to read final state
            handler = ::writeToSockets,
        )

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

    private fun writeToSockets(event: Event) {
        if ((event as WebSocketEvent).serializeAsync) {
            Util.backgroundExecutor().execute { serializeAndBroadcast(event) }
        } else {
            serializeAndBroadcast(event)
        }
    }

    private fun serializeAndBroadcast(event: Event) {
        val json = try {
            val writer = writeBuffer.get()
            JsonWriter(writer).use { writer ->
                writer.beginObject()
                writer.name("name").value(event.javaClass.eventName)
                writer.name("event")
                (event as WebSocketEvent).serializer.toJson(event, event.javaClass, writer)
                writer.endObject()
            }
            writer.toString().also { writer.builder.clear() }
        } catch (e: Exception) {
            logger.error("Failed to serialize event $event", e)
            return
        }

        httpServer.webSocketController!!.broadcast(json) { _, t ->
            logger.error("WebSocket event broadcast failed, event: ${event.javaClass.eventName}", t)
        }
    }

}
