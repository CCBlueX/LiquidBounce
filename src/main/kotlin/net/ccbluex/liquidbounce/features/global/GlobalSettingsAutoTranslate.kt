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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslateLanguage
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslatorApi
import net.ccbluex.liquidbounce.api.thirdparty.translator.providers.GoogleTranslateApi
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import java.time.Duration

private data class TranslationCacheKey(
    val sourceLanguage: String,
    val targetLanguage: String,
    val text: String,
)

object GlobalSettingsAutoTranslate : ValueGroup(name = "AutoTranslate"), TranslatorApi, EventListener {

    private val translationLazyCache = lazy<Cache<TranslationCacheKey, TranslationResult.Success>> {
        CacheBuilder.newBuilder()
            .maximumSize(512)
            .expireAfterWrite(Duration.ofMinutes(15))
            .build()
    }

    private val translationCache by translationLazyCache

    private val providers = modes(this, "Provider", 0) {
        arrayOf(
            GoogleTranslateApi(it)
        )
    }

    init {
        providers.onChanged {
            if (translationLazyCache.isInitialized()) {
                translationCache.invalidateAll()
            }
        }
    }

    override suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult {
        val key = TranslationCacheKey(
            sourceLanguage.literal,
            targetLanguage.literal,
            text,
        )

        translationCache.getIfPresent(key)?.let { return it }

        val result = providers.activeMode.translateInternal(sourceLanguage, targetLanguage, text)

        if (result is TranslationResult.Success) {
            translationCache.put(key, result)
        }

        return result
    }
}
