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
package net.ccbluex.liquidbounce.api.thirdparty.ai

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.ANTHROPIC_API_VERSION
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.ANTHROPIC_MAX_TOKENS
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.AnthropicMessage
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.AnthropicRequest
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.AnthropicService
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.OpenAiChatRequest
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.OpenAiCompatibleService
import net.ccbluex.liquidbounce.api.thirdparty.ai.service.OpenAiMessage
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.utils.client.logger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

/**
 * Requests AI chat completions over one of [AiProtocol]'s wire formats.
 *
 * @param protocol The wire format to speak
 * @param baseUrl API root, falls back to [AiProtocol.defaultBaseUrl] when left blank
 * @param apiKey Credential sent in the protocol's auth header
 * @param model Model identifier passed to the provider
 * @param systemPrompt System instruction prepended to every request
 */
class AiChatClient(
    private val protocol: AiProtocol,
    baseUrl: String = "",
    private val apiKey: String,
    private val model: String,
    private val systemPrompt: String,
) {

    private val retrofit: Retrofit = Retrofit.Builder()
        // Retrofit requires the base URL to end with a slash to resolve endpoint paths against it.
        .baseUrl((baseUrl.ifBlank { protocol.defaultBaseUrl }).let { if (it.endsWith('/')) it else "$it/" })
        .client(HttpClient.client)
        .addConverterFactory(GSON)
        .build()

    private val openAiCompatible: OpenAiCompatibleService by lazy { retrofit.create() }

    private val anthropic: AnthropicService by lazy { retrofit.create() }

    /**
     * Requests a new answer from the AI
     *
     * @param question The question to ask
     * @return The response from the AI
     */
    suspend fun requestNewAnswer(question: String): String = try {
        when (protocol) {
            AiProtocol.OPENAI_COMPATIBLE -> requestOpenAiCompatible(question)
            AiProtocol.ANTHROPIC -> requestAnthropic(question)
        }
    } catch (e: CancellationException) {
        // The caller cancelled (e.g. module disabled), this is not a request failure.
        throw e
    } catch (e: Exception) {
        throw e.asAiException()
    }

    private suspend fun requestOpenAiCompatible(question: String): String = openAiCompatible.chatCompletions(
        // Local endpoints usually do not authenticate and must not receive an empty bearer token.
        authorization = apiKey.takeIf(String::isNotBlank)?.let { "Bearer $it" },
        body = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage("system", systemPrompt),
                OpenAiMessage("user", question),
            ),
        ),
    ).choices?.firstOrNull()?.message?.content
        ?: throw AiRequestException("${protocol.tag} returned no answer")

    private suspend fun requestAnthropic(question: String): String = anthropic.messages(
        apiKey = apiKey,
        version = ANTHROPIC_API_VERSION,
        body = AnthropicRequest(
            model = model,
            maxTokens = ANTHROPIC_MAX_TOKENS,
            system = systemPrompt,
            messages = listOf(AnthropicMessage("user", question)),
        ),
    ).content
        ?.filter { it.type == "text" }
        ?.mapNotNull { it.text }
        ?.joinToString("")
        ?.takeIf(String::isNotEmpty)
        ?: throw AiRequestException("${protocol.tag} returned no answer")

    private fun Throwable.asAiException(): Exception {
        // HttpClient rejects every non-2xx response, which OkHttp hands to a suspending call wrapped in an IOException.
        val http = httpException
        if (http == null) {
            logger.error("Failed to send request to ${protocol.tag}", this)
            return this as? Exception ?: Exception(this)
        }

        val message = extractErrorMessage(http.content)
        logger.error("${protocol.tag} returned an error (${http.code}): $message", this)
        return AiRequestException("${protocol.tag} returned an error: $message", this)
    }

    private companion object {

        /**
         * Dedicated converter: field names are pinned with
         * [com.google.gson.annotations.SerializedName], so the client config naming strategy must not apply here.
         */
        private val GSON = GsonConverterFactory.create(GSON_DEFAULT)

        const val MAX_ERROR_MESSAGE_LENGTH = 200

        /**
         * Both protocols report failures as `{"error": {"message": "..."}}`. Anything else
         * (HTML error pages, proxy messages) is passed through truncated.
         */
        fun extractErrorMessage(content: String): String = runCatching {
            val error = JsonParser.parseString(content).asJsonObject["error"]
            if (error.isJsonObject) error.asJsonObject["message"]?.asString else error.asString
        }.getOrNull()?.takeIf(String::isNotBlank) ?: content.take(MAX_ERROR_MESSAGE_LENGTH)

    }

}

/**
 * Thrown when the provider rejected the request or returned an answer that could not be read.
 */
class AiRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)
