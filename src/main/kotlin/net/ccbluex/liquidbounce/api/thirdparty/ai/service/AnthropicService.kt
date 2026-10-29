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
package net.ccbluex.liquidbounce.api.thirdparty.ai.service

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Required `anthropic-version` header value,
 * see [Anthropic versioning](https://docs.anthropic.com/en/api/versioning).
 */
const val ANTHROPIC_API_VERSION = "2023-06-01"

/**
 * `max_tokens` is mandatory for the Messages API and has no server-side default.
 */
const val ANTHROPIC_MAX_TOKENS = 1024

/**
 * Anthropic Messages wire protocol.
 *
 * See [Anthropic messages](https://docs.anthropic.com/en/api/messages).
 */
interface AnthropicService {

    @POST("messages")
    suspend fun messages(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Body body: AnthropicRequest,
    ): AnthropicResponse

}

/**
 * Unlike OpenAI, the system prompt is a top-level field instead of a message with the
 * `system` role.
 */
data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>,
)

data class AnthropicMessage(
    val role: String,
    val content: String,
)

data class AnthropicResponse(
    val content: List<AnthropicContent>?,
) {

    /**
     * Only `text` blocks carry the answer; other block types (e.g. `tool_use`) are ignored.
     */
    data class AnthropicContent(
        val type: String?,
        val text: String?,
    )

}
