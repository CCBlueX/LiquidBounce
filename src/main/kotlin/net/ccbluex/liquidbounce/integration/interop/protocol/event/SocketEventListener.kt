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

import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer.httpServer
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.reflect.KClass

class SocketEventListener : EventListener {

    private val events = ALL_EVENT_CLASSES
        .filter { it.java.isAnnotationPresent(WebSocketEvent::class.java) }
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
        data class WSEventData(val name: String, val event: JsonElement)

        val json = runCatching {
            val webSocketAnnotation = event::class.java.getAnnotation(WebSocketEvent::class.java)!!
            interopGson.toJson(
                WSEventData(
                    name = event::class.eventName,
                    event = webSocketAnnotation.serializer.gson.toJsonTree(event)
                )
            )
        }.onFailure {
            logger.error("Failed to serialize event $event", it)
        }.getOrNull() ?: return@withScope

        httpServer.webSocketController.broadcast(json) { channelHandlerContext, t ->
            logger.error("WebSocket event broadcast failed", t)
        }
    }



}
