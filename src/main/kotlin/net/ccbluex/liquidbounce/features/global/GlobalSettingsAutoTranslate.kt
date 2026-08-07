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

package net.ccbluex.liquidbounce.features.global

import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslateLanguage
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslatorApi
import net.ccbluex.liquidbounce.api.thirdparty.translator.providers.GoogleTranslateApi
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.collection.LruCache

private data class TranslationKey(val sourceLanguage: String, val targetLanguage: String, val text: String)

object GlobalSettingsAutoTranslate : ValueGroup(name = "AutoTranslate"), TranslatorApi, EventListener {

    private val cache = LruCache<TranslationKey, TranslationResult>(10_000)
    private val providers = modes(this, "Provider", 0) {
        arrayOf(
            GoogleTranslateApi(it)
        )
    }

    override suspend fun translate(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult {
        val key = TranslationKey(sourceLanguage.literal, targetLanguage.literal, text)
        cache[key]?.let { return it }

        val result = super.translate(sourceLanguage, targetLanguage, text)

        if (result.isValid) {
            cache.put(key, result)
        }

        return result
    }

    override suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult {
        return providers.activeMode.translateInternal(
            sourceLanguage,
            targetLanguage,
            text
        )
    }
}
