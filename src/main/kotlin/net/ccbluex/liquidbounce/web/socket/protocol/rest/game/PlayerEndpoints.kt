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
package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

internal fun RestNode.setupPlayerRestApi() {
    get("/player") {
        val player = mc.player ?: return@get httpForbidden("Player is null")

        httpOk(JsonObject().apply {
            addProperty("health", player.health)
            addProperty("maxHealth", player.maxHealth)
            addProperty("food", player.hungerManager.foodLevel)
            addProperty("experienceProgress", player.experienceProgress)
            addProperty("dead", player.isDead)
        })
    }

    get("/player/position") {
        val player = mc.player ?: return@get httpForbidden("Player is null")

        httpOk(JsonObject().apply {
            addProperty("x", player.x)
            addProperty("y", player.y)
            addProperty("z", player.z)
        })
    }

    get("/player/rotation") {
        val player = mc.player ?: return@get httpForbidden("Player is null")

        httpOk(JsonObject().apply {
            addProperty("yaw", player.yaw)
            addProperty("pitch", player.pitch)
        })
    }

    get("/player/velocity") {
        val player = mc.player ?: return@get httpForbidden("Player is null")

        httpOk(JsonObject().apply {
            addProperty("x", player.velocity.x)
            addProperty("y", player.velocity.y)
            addProperty("z", player.velocity.z)
        })
    }

    // Send chat message
    post("/player/sendChatMessage") {
        data class ChatMessageRequest(val message: String)

        val messageRequest = decode<ChatMessageRequest>(it.content)
        network.sendChatMessage(messageRequest.message)
        httpOk(JsonObject())
    }

    post("/player/sendCommand") {
        data class CommandRequest(val message: String)

        val commandRequest = decode<CommandRequest>(it.content)
        network.sendChatCommand(commandRequest.message)
        httpOk(JsonObject())
    }
}
