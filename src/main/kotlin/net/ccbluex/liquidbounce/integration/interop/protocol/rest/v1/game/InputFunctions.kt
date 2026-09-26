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
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.utils.client.mc

// GET /api/v1/client/input
private fun Route.getInputInfo() = get("/input") {
    val key = call.queryParameters["key"] ?: call.badRequest("Missing key parameter")
    val input = InputConstants.getKey(key)

    call.respond(JsonObject().apply {
        addProperty("translationKey", input.name)
        addProperty("localized", input.displayName.string)
    })
}

// GET /api/v1/client/keybinds
private fun Route.getKeybinds() = get("/keybinds") {
    call.respond(
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
@Volatile
var isTyping = false

private data class TypingState(val typing: Boolean)

// POST /api/v1/client/typing
private fun Route.isTyping() = post("/typing") {
    val typingRequest = call.receive<TypingState>()
    isTyping = typingRequest.typing

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// GET /api/v1/client/typing
private fun Route.getIsTyping() = get("/typing") {
    call.respond(TypingState(isTyping))
}

internal fun Route.inputRoutes() {
    getInputInfo()
    getKeybinds()
    isTyping()
    getIsTyping()
}
