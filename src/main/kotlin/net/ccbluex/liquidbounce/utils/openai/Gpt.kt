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

import com.google.gson.JsonParser
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder

class Gpt(val openAiKey: String, val model: String, val prompt: String) {

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
        val httpClient = HttpClientBuilder.create().build()

        // Send request to https://api.openai.com/v1/chat/completions
        val httpPost = HttpPost("https://api.openai.com/v1/chat/completions")
        httpPost.addHeader("Authorization", "Bearer $openAiKey")
        httpPost.addHeader("Content-Type", "application/json")

        val json = """
            {
                "model": "$model",
                "messages": [
                    {
                        "role": "system",
                        "content": "$prompt"
                    },
                    {
                        "role": "user",
                        "content": "$question"
                    }
                ]
            }
        """.trimIndent()

        // Set entity to JSON body
        httpPost.entity = EntityBuilder.create().setText(json).build()

        // Send request
        val response = httpClient.execute(httpPost)
        val responseBody = response.entity.content.readBytes().toString(Charsets.UTF_8)
        response.close()

        // Parse OpenAI response
        val responseJson = JsonParser.parseString(responseBody).asJsonObject

        return responseJson["choices"].asJsonArray[0].asJsonObject["message"].asJsonObject["content"].asString
    }

}
