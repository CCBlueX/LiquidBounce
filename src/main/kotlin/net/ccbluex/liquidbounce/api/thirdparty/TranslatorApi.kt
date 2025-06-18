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
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.authlib.utils.array
import net.ccbluex.liquidbounce.authlib.utils.string
import net.ccbluex.liquidbounce.features.command.commands.translate.CommandAutoTranslate
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.MutableText
import okhttp3.HttpUrl.Companion.toHttpUrl

object TranslatorApi {

    data class TranslationResult(
        val origin: String,
        val translation: String,
        val fromLanguage: String,
        val toLanguage: String,
    ) {
        val isValid = origin != translation && fromLanguage != toLanguage

        fun toResultText(): MutableText = "".asText()
            .append(regular("("))
            .append(variable(fromLanguage))
            .append(regular("->"))
            .append(variable(toLanguage))
            .append(regular(") "))
            .append(regular(translation).copyable(copyContent = translation))
    }

    private val googleApiUrl = "https://translate.googleapis.com/translate_a/t?client=gtx&dt=t".toHttpUrl()

    /**
     * [Reference](https://github.com/ssut/py-googletrans/issues/268)
     * Updated at 2025/06/11
     */
    suspend fun google(
        sourceLanguage: String = "auto",
        targetLanguage: String = CommandAutoTranslate.languageCode,
        text: String,
    ): TranslationResult {
        require(sourceLanguage.isNotBlank() && targetLanguage.isNotBlank()) { "Language cannot be blank" }
        require(text.isNotBlank()) { "Text cannot be blank" }

        // POST with Form body or GET with URL query params
        val url = googleApiUrl.newBuilder()
            .addQueryParameter("sl", sourceLanguage)
            .addQueryParameter("tl", targetLanguage)
            .addQueryParameter("q", text)
            .build().toString()
        val response = HttpClient.request(
            url,
            method = HttpMethod.GET,
        )

        // 1. sl = "auto"
        // Model: [["$result", "$detectedLanguage"]]
        // 2. sl specified
        // Model: ["$result"]

        // tl invalid -> translate into English
        // sl invalid -> result equals text

        // sl empty -> HTTP 400
        // tl empty | text empty -> result empty
        return if (sourceLanguage == "auto") {
            val arr = response.parse<JsonArray>().array(0)!!
            val result = arr.string(0)!!
            val detectedLanguage = arr.string(1)!!
            TranslationResult(
                origin = text,
                translation = result,
                fromLanguage = detectedLanguage,
                toLanguage = targetLanguage,
            )
        } else {
            val result = response.parse<JsonArray>().string(0)!!
            TranslationResult(
                origin = text,
                translation = result,
                fromLanguage = sourceLanguage,
                toLanguage = targetLanguage,
            )
        }
    }

}
