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

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI Chat Completions wire protocol.
 *
 * See [OpenAI chat completions](https://platform.openai.com/docs/api-reference/chat/create).
 */
interface OpenAiCompatibleService {

    @POST("chat/completions")
    suspend fun chatCompletions(
        /**
         * `null` omits the header, which local endpoints such as Ollama usually require.
         */
        @Header("Authorization") authorization: String?,
        @Body body: OpenAiChatRequest,
    ): OpenAiChatResponse

}

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
)

data class OpenAiMessage(
    val role: String,
    val content: String,
)

data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>?,
) {

    data class OpenAiChoice(
        val message: OpenAiMessage?,
    )

}
