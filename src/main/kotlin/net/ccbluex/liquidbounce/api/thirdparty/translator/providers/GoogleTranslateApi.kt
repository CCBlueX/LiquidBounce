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

package net.ccbluex.liquidbounce.api.thirdparty.translator.providers

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslateLanguage
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslatorChoice
import net.ccbluex.liquidbounce.authlib.utils.array
import net.ccbluex.liquidbounce.authlib.utils.string
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import okhttp3.HttpUrl.Companion.toHttpUrl

private val GOOGLE_API_URL = "https://translate.googleapis.com/translate_a/t?client=gtx&dt=t".toHttpUrl()

/**
 * @author MukjepScarlet
 */
class GoogleTranslateApi(
    override val parent: ChoiceConfigurable<*>
) : TranslatorChoice("Google") {
    /**
     * [Reference](https://github.com/ssut/py-googletrans/issues/268)
     * Updated at 2025/06/11
     */
    override suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult {
        val url = GOOGLE_API_URL.newBuilder()
            .addQueryParameter("sl", sourceLanguage.literal)
            .addQueryParameter("tl", targetLanguage.literal)
            .addQueryParameter("q", text)
            .build()
            .toString()

        val response = HttpClient.request(
            url,
            method = HttpMethod.GET
        )

        // 1. sl = "auto"
        // Model: [["$result", "$detectedLanguage"]]
        // 2. sl specified
        // Model: ["$result"]

        // tl invalid -> translate into English
        // sl invalid -> result equals text

        // sl empty -> HTTP 400
        // tl empty | text empty -> result empty
        return if (sourceLanguage is TranslateLanguage.Auto) {
            val arr = response.parse<JsonArray>().array(0)!!
            val result = arr.string(0)!!
            val detectedLanguage = arr.string(1)!!
            TranslationResult.Success(
                origin = text,
                translation = result,
                fromLanguage = TranslateLanguage.of(detectedLanguage),
                toLanguage = targetLanguage,
            )
        } else {
            val result = response.parse<JsonArray>().string(0)!!
            TranslationResult.Success(
                origin = text,
                translation = result,
                fromLanguage = sourceLanguage,
                toLanguage = targetLanguage
            )
        }
    }
}
