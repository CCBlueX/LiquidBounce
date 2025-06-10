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

import net.ccbluex.liquidbounce.api.core.BaseApi
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.tika.Tika
import java.io.File

class LibreTranslateApi(baseUrl: String = "https://libretranslate.com") : BaseApi(baseUrl) {

    private val tika by lazy(::Tika)

    private fun FormBody.Builder.addOptional(name: String, value: String?) =
        if (value != null) add(name, value) else this

    private fun FormBody.Builder.addEncodedOptional(name: String, value: String?) =
        if (value != null) addEncoded(name, value) else this

    fun MultipartBody.Builder.addFormDataPartOptional(name: String, value: String?) =
        if (value != null) addFormDataPart(name, value) else this

    suspend fun detect(
        text: String,
        apiKey: String? = null,
    ): List<DetectionResult> = post(
        "/detect",
        body = FormBody.Builder()
            .addEncoded("q", text)
            .addEncodedOptional("api_key", apiKey)
            .build(),
    )

    suspend fun languages(): List<Language> = get("/languages")

    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        format: String = "text",
        alternatives: Int = 0,
        apiKey: String? = null,
    ): TranslationResponse = post(
        "/translate",
        body = FormBody.Builder()
            .addEncoded("q", text)
            .addEncoded("source", sourceLanguage)
            .addEncoded("target", targetLanguage)
            .addEncoded("format", format)
            .addEncoded("alternatives", alternatives.toString())
            .addEncodedOptional("api_key", apiKey)
            .build(),
    )

    suspend fun translateFile(
        file: File,
        sourceLanguage: String,
        targetLanguage: String,
        apiKey: String? = null,
    ): FileTranslationResponse = internalTranslateFile(
        fileBody = file.asRequestBody(tika.detect(file).toMediaTypeOrNull()),
        fileName = file.name,
        sourceLanguage,
        targetLanguage,
        apiKey,
    )

    suspend fun frontendSettings(): FrontendSettings = get("/frontend/settings")

    suspend fun suggest(
        originalText: String,
        suggestedTranslation: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): SuggestionResponse = post(
    "/suggest",
        body = FormBody.Builder()
            .addEncoded("q", originalText)
            .addEncoded("s", suggestedTranslation)
            .addEncoded("source", sourceLanguage)
            .addEncoded("target", targetLanguage)
            .build(),
    )

    private suspend fun internalTranslateFile(
        fileBody: RequestBody,
        fileName: String? = null,
        sourceLanguage: String,
        targetLanguage: String,
        apiKey: String? = null,
    ): FileTranslationResponse = post(
        "/translate_file",
        body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .addFormDataPart("source", sourceLanguage)
            .addFormDataPart("target", targetLanguage)
            .addFormDataPartOptional("api_key", apiKey)
            .build(),
    )

    data class DetectionResult(
        val confidence: Int,
        val language: String
    )

    data class Language(
        val code: String,
        val name: String,
        val targets: List<String>
    )

    data class TranslationResponse(
        val translatedText: String
    )

    data class FileTranslationResponse(
        val translatedFileUrl: String
    )

    data class FrontendSettings(
        val apiKeys: Boolean,
        val charLimit: Int,
        val frontendTimeout: Int,
        val keyRequired: Boolean,
        val language: LanguageSettings,
        val suggestions: Boolean,
        val supportedFilesFormat: List<String>
    )

    data class LanguageSettings(
        val source: LanguageCodeName,
        val target: LanguageCodeName
    )

    data class LanguageCodeName(
        val code: String,
        val name: String
    )

    data class SuggestionResponse(
        val success: Boolean
    )

    data class ErrorResponse(
        val error: String
    )

}
