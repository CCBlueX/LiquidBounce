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
 */
package net.ccbluex.liquidbounce.utils.openai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.utils.io.HttpClient

const val OPENAI_BASE_URL = "https://api.openai.com/v1"

/**
 * OpenAI API
 */
class OpenAi(
    private val baseUrl: String = OPENAI_BASE_URL,
    private val openAiKey: String,
    private val model: String,
    private val prompt: String
) {

    /**
     * {
     *     "model": "gpt-3.5-turbo",
     *     "messages": [
     *       {
     *         "role": "system",
     *         "content": "You are a helpful assistant."
     *       },
     *       {
     *         "role": "user",
     *         "content": "Hello!"
     *       }
     *     ]
     *   }
     */
    fun requestNewAnswer(question: String): String {
        val systemRole = JsonObject()
        systemRole.addProperty("role", "system")
        systemRole.addProperty("content", prompt)

        val userRole = JsonObject()
        userRole.addProperty("role", "user")
        userRole.addProperty("content", question)

        val messages = JsonArray()
        messages.add(systemRole)
        messages.add(userRole)

        val body = JsonObject()
        body.addProperty("model", model)
        body.add("messages", messages)

        val json = body.toString()

        // Send request
        val (code, text) = HttpClient.requestWithCode("$baseUrl/chat/completions",
            "POST",
            headers = arrayOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $openAiKey"
            ),
            inputData = json.toByteArray()
        )

        val responseJson = JsonParser.parseString(text).asJsonObject

        if (code == 200) {
            return responseJson["choices"]
                .asJsonArray[0]
                .asJsonObject["message"]
                .asJsonObject["content"]
                .asString
        } else {
            /**
             * {
             *     "error": {
             *         "message": "Incorrect API key provided: dasdasdw.
             *         You can find your API key at https://platform.openai.com/account/api-keys.",
             *         "type": "invalid_request_error",
             *         "param": null,
             *         "code": "invalid_api_key"
             *     }
             * }
             */
            val errorJson = responseJson["error"].asJsonObject

            error("OpenAI returned an error: ${errorJson["message"].asString}")
        }
    }

}
