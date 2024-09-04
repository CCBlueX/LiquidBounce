/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.socket.protocol.event

import com.google.gson.JsonObject
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.socket.ClientServer.httpServer
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import kotlin.reflect.KClass

class SocketEventHandler : Listenable {

    private val events
        get() = ALL_EVENT_CLASSES
            .filter { it.java.isAnnotationPresent(WebSocketEvent::class.java) }
            .associateBy { it.eventName }

    /**
     * Contains all events that are registered in the current context
     */
    private val registeredEvents = mutableMapOf<KClass<out Event>, EventHook<in Event>>()

    fun registerAll() {
        events.keys.forEach { register(it) }
    }

    fun register(name: String) {
        val eventClass = events[name] ?:
            throw IllegalArgumentException("Unknown event: $name")

        if (registeredEvents.containsKey(eventClass)) {
            error("Event $name is already registered")
        }

        val eventHook = EventHook<Event>(
            this,
            { writeToSockets(it) },
            false
        )

        registeredEvents[eventClass] = eventHook
        EventManager.registerEventHook(eventClass.java, eventHook)
    }

    fun unregister(name: String) {
        val (eventClass, eventHook) = registeredEvents.entries.find { it.key.eventName == name } ?:
            throw IllegalArgumentException("Unknown event: $name")

        EventManager.unregisterEventHook(eventClass.java, eventHook)
    }

    private fun writeToSockets(event: Event) {
        val json = runCatching {
            val jsonObj = JsonObject()
            jsonObj.addProperty("name", event::class.eventName)
            jsonObj.add("event", protocolGson.toJsonTree(event))
            protocolGson.toJson(jsonObj)
        }.onFailure {
            logger.error("Failed to serialize event $event", it)
        }.getOrNull() ?: return

        httpServer.webSocketController.broadcast(json)
    }



}
