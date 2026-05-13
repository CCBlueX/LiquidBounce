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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.routing.RoutingContext

// GET /api/v1/client/input
fun RoutingContext.getInputInfo() {
    val key = queryParameters["key"] ?: badRequest("Missing key parameter")
    val input = InputConstants.getKey(key)

    respond(JsonObject().apply {
        addProperty("translationKey", input.name)
        addProperty("localized", input.displayName.string)
    })
}

// GET /api/v1/client/keybinds
fun RoutingContext.getKeybinds() {
    respond(
        JsonArray().apply {
            for (key in mc.options.keyMappings) {
                add(JsonObject().apply {
                    addProperty("bindName", key.name)
                    add("key", JsonObject().apply {
                        addProperty("translationKey", key.saveString())
                        addProperty("localized", key.translatedKeyMessage?.string)
                    })
                })
            }
        }
    )
}

/**
 * Keeps track if we are currently typing in a text field
 */
var isTyping = false

// POST /api/v1/client/typing
fun RoutingContext.isTyping() {
    data class TypingRequest(val typing: Boolean)

    val typingRequest = receive<TypingRequest>()
    isTyping = typingRequest.typing

    respondNoContent()
}

// GET /api/v1/client/typing
fun RoutingContext.getIsTyping() {
    respond(JsonObject().apply {
        addProperty("typing", isTyping)
    })
}
