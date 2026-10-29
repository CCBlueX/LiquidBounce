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

package net.ccbluex.liquidbounce.api.thirdparty.translator.providers

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.bodyAsJson
import net.ccbluex.liquidbounce.api.core.toRequestBody
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslateLanguage
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslatorMode
import net.ccbluex.liquidbounce.config.gson.util.jsonObject
import net.ccbluex.liquidbounce.config.gson.util.obj
import net.ccbluex.liquidbounce.config.gson.util.string
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup

/**
 * Translator provider for [LibreTranslate](https://libretranslate.com/docs/) instances, which can be
 * self-hosted and therefore has to be configured by the user.
 *
 * Requests are issued through the shared [HttpClient] instead of Retrofit, because a Retrofit service is
 * bound to a single base URL while [baseUrl] is user-configurable at runtime.
 *
 * @see <a href="https://libretranslate.com/docs/#/translate/post_translate">LibreTranslate: POST /translate</a>
 */
class LibreTranslateApi(
    override val parent: ModeValueGroup<*>
) : TranslatorMode("LibreTranslate") {

    /**
     * Base URL of the instance without the `/translate` path, for example `https://libretranslate.com`.
     */
    private val baseUrl by text("BaseUrl", "https://libretranslate.com")

    /**
     * API key of the instance. Left blank for instances that do not require one.
     */
    private val apiKey by text("ApiKey", "")

    override suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        val payload = jsonObject {
            "q"(text)
            "source"(sourceLanguage.toLibreCode())
            "target"(targetLanguage.toLibreCode())
            "format"("text")

            if (apiKey.isNotBlank()) {
                "api_key"(apiKey)
            }
        }

        val response = HttpClient.request(
            url = "${baseUrl.trim().trimEnd('/')}/translate",
            method = HttpMethod.POST,
            body = payload.toRequestBody(),
        ).bodyAsJson<JsonObject>()

        // Model: {"detectedLanguage": {"confidence": 100, "language": "$detectedLanguage"}, "$result"}
        // `detectedLanguage` is only present when the source language is "auto".
        // Unknown source or target language -> HTTP 400 with {"error": "$reason"}
        val translatedText = response.string("translatedText")
            ?: error("LibreTranslate response has no translatedText: $response")
        val detectedLanguage = response.obj("detectedLanguage")?.string("language")

        TranslationResult.Success(
            origin = text,
            translation = translatedText,
            fromLanguage = detectedLanguage?.let { TranslateLanguage.of(it) } ?: sourceLanguage,
            toLanguage = targetLanguage,
        )
    }

    /**
     * LibreTranslate only knows language codes without a region subtag (e.g. `en` instead of `en-US`)
     * and names traditional Chinese `zt`.
     */
    private fun TranslateLanguage.toLibreCode(): String {
        val subtags = literal.lowercase().split('-', '_')
        val primaryLanguage = subtags.first()

        return if (primaryLanguage == "zh" && subtags.any { it == "tw" || it == "hk" || it == "mo" || it == "hant" }) {
            "zt"
        } else {
            primaryLanguage
        }
    }

}
