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
 *
 */

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.client.util.InputUtil

// GET /api/v1/client/input
@Suppress("UNUSED_PARAMETER")
fun getInputInfo(requestObject: RequestObject) = requestObject.queryParams["key"]?.let { key ->
    val input = InputUtil.fromTranslationKey(key)

    httpOk(JsonObject().apply {
        addProperty("translationKey", input.translationKey)
        addProperty("localized", input.localizedText.convertToString())
    })
} ?: httpBadRequest("Missing key parameter")

// GET /api/v1/client/keybinds
@Suppress("UNUSED_PARAMETER")
fun getKeybinds(requestObject: RequestObject) = httpOk(
    JsonArray().apply {
        for (key in mc.options.allKeys) {
            add(JsonObject().apply {
                addProperty("bindName", key.translationKey)
                add("key", JsonObject().apply {
                    addProperty("translationKey", key.boundKeyTranslationKey)
                    addProperty("localized", key.boundKeyLocalizedText?.convertToString())
                })
            })
        }
    }
)

/**
 * Keeps track if we are currently typing in a text field
 */
var isTyping = false

// POST /api/v1/client/typing
fun isTyping(requestObject: RequestObject): FullHttpResponse {
    data class TypingRequest(val typing: Boolean)

    val typingRequest = requestObject.asJson<TypingRequest>()
    isTyping = typingRequest.typing

    return httpNoContent()
}

// GET /api/v1/client/typing
@Suppress("UNUSED_PARAMETER")
fun getIsTyping(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("typing", isTyping)
})
