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
 */
package net.ccbluex.liquidbounce.api.thirdparty

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.core.toRequestBody
import net.ccbluex.liquidbounce.utils.client.logger

const val OPENAI_BASE_URL = "https://api.openai.com/v1"

/**
 * OpenAI API
 */
class OpenAiApi(
    baseUrl: String = OPENAI_BASE_URL,
    private val openAiKey: String,
    private val model: String,
    private val prompt: String
) : BaseApi(baseUrl) {

    /**
     * Requests a new answer from the OpenAI API
     *
     * @param question The question to ask
     * @return The response from the AI
     */
    suspend fun requestNewAnswer(question: String): String {
        val systemRole = JsonObject().apply {
            addProperty("role", "system")
            addProperty("content", prompt)
        }

        val userRole = JsonObject().apply {
            addProperty("role", "user")
            addProperty("content", question)
        }

        val messages = JsonArray().apply {
            add(systemRole)
            add(userRole)
        }

        val body = JsonObject().apply {
            addProperty("model", model)
            add("messages", messages)
        }

        return try {
            val response: JsonObject = post(
                "/chat/completions",
                body = body.toRequestBody()
            ) {
                add("Authorization", "Bearer $openAiKey")
            }

            response["choices"]
                .asJsonArray[0]
                .asJsonObject["message"]
                .asJsonObject["content"]
                .asString
        } catch (e: HttpException) {
            val responseJson = JsonParser.parseString(e.content).asJsonObject
            val errorJson = responseJson["error"].asJsonObject

            logger.error("Failed to send request to OpenAI", e)
            error("OpenAI returned an error: ${errorJson["message"].asString}")
        } catch (e: Exception) {
            logger.error("Failed to send request to OpenAI", e)
            throw e
        }
    }

}
