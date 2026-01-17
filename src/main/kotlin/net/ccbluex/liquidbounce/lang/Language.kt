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

/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.lang

import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientLanguageChangedEvent
import net.ccbluex.liquidbounce.lang.LanguageManager.knownLanguages
import net.ccbluex.liquidbounce.lang.LanguageManager.languageMap
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.StringDecomposer
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

fun translation(key: String, vararg args: Any): MutableComponent =
    MutableComponent.create(LanguageText(key, args))

object LanguageManager : Configurable("lang") {

    // Current language
    val languageIdentifier: String
        get() = overrideLanguage.ifBlank { mc.options.languageCode }

    // The game language can be overridden by the user
    var overrideLanguage by text("OverrideLanguage", "").onChanged { lang ->
        loadLanguage(lang)
        EventManager.callEvent(ClientLanguageChangedEvent())
    }

    // Common language
    private const val COMMON_UNDERSTOOD_LANGUAGE = "en_us"

    // List of all languages
    val knownLanguages = setOf(
        "en_us",
        "de_de",
        "ja_jp",
        "zh_cn",
        "zh_tw",
        "ru_ru",
        "ua_ua",
        "en_pt",
        "pt_br",
        "tr_tr",
        "nl_nl",
        "nl_be"
    )
    private val languageMap = ConcurrentHashMap<String, ClientLanguage>()

    /**
     * Load a specified language which are pre-defined in [knownLanguages] and stored in assets.
     * If a language is not found, it will be logged as error.
     *
     * Languages are stored in assets/minecraft/liquidbounce/lang and when loaded will be stored in [languageMap]
     */
    private fun loadLanguage(language: String): ClientLanguage? {
        return if (languageMap.containsKey(language)) {
            languageMap[language]!!
        } else if (language !in knownLanguages) {
            loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)
        } else {
            runCatching {
                languageMap.computeIfAbsent(language) {
                    val languageFile = javaClass.getResourceAsStream("/resources/liquidbounce/lang/$language.json")
                    val translations = languageFile!!.readJson<HashMap<String, String>>()

                    ClientLanguage(translations)
                }
            }.onSuccess {
                logger.info("Loaded language $language")
            }.onFailure {
                logger.error("Failed to load language $language", it)
            }.getOrNull()
        }
    }

    fun loadDefault() {
        loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)
        loadLanguage(languageIdentifier)
    }

    fun getLanguage() = loadLanguage(languageIdentifier) ?: loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)

    fun getCommonLanguage() = loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)

    fun hasFallbackTranslation(key: String) =
        loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)?.has(key) ?: false

}

class ClientLanguage(private val translations: Map<String, String>) : Language() {

    private fun getTranslation(key: String) = translations[key]

    /**
     * Get a translation for the given key.
     * If the translation is not found, the fallback will be used.
     * If the fallback is not found, the key will be returned.
     *
     * Be careful when using this method that it will not cause a stack overflow.
     * Use [getTranslation] instead.
     */
    override fun getOrDefault(key: String, fallback: String) = getTranslation(key)
        ?: LanguageManager.getCommonLanguage()?.getTranslation(key)
        ?: fallback

    override fun has(key: String) = translations.containsKey(key)

    override fun isDefaultRightToLeft() = false

    override fun getVisualOrder(text: FormattedText) = FormattedCharSequence { visitor ->
        text.visit({ style, string ->
            if (StringDecomposer.iterateFormatted(string, style, visitor)) {
                Optional.empty()
            } else {
                FormattedText.STOP_ITERATION
            }
        }, Style.EMPTY).isPresent
    }

}
