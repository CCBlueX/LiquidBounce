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
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientLanguageChangedEvent
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

object LanguageManager : ValueGroup("Language") {

    var clientLanguage by enumChoice("ClientLanguage", ClientLanguage.AUTO)
        .onChanged { _ ->
            loadLanguage(currentLanguageChoice())
            EventManager.callEvent(ClientLanguageChangedEvent())
        }

    private val COMMON_UNDERSTOOD_LANGUAGE = ClientLanguage.EN_US
    val MINECRAFT_LANGUAGE: ClientLanguage?
        get() = languageChoiceFromCode(mc.options.languageCode)

    enum class ClientLanguage(override val tag: String, val code: String? = null) : Tagged {
        AUTO("Auto"),
        EN_US("English (US)", "en_us"),
        EN_PT("English (Pirate)", "en_pt"),
        DE_DE("German", "de_de"),
        JA_JP("Japanese", "ja_jp"),
        ZH_CN("Chinese (Simplified)", "zh_cn"),
        ZH_TW("Chinese (Traditional)", "zh_tw"),
        RU_RU("Russian", "ru_ru"),
        UA_UA("Ukrainian", "ua_ua"),
        PT_BR("Portuguese (Brazil)", "pt_br"),
        TR_TR("Turkish", "tr_tr"),
        NL_NL("Dutch (Netherlands)", "nl_nl"),
        NL_BE("Dutch (Belgium)", "nl_be"),
    }

    val languageCodes = ClientLanguage.entries
        .mapNotNull(ClientLanguage::code)
        .toSet()

    private val languageRegistry = ConcurrentHashMap<ClientLanguage, net.ccbluex.liquidbounce.lang.ClientLanguage>()

    private fun loadLanguage(choice: ClientLanguage): net.ccbluex.liquidbounce.lang.ClientLanguage? {
        require(choice != ClientLanguage.AUTO) { "Cannot load language ${choice.code} because it is auto" }
        require(choice.code != null) { "Cannot load language ${choice.tag} because it has no code" }

        return if (languageRegistry.containsKey(choice)) {
            languageRegistry[choice]!!
        } else {
            runCatching {
                languageRegistry.computeIfAbsent(choice) {
                    val languageFile = javaClass.getResourceAsStream(
                        "/resources/liquidbounce/lang/${choice.code}.json"
                    )
                    val translations = languageFile!!.readJson<HashMap<String, String>>()

                    ClientLanguage(translations)
                }
            }.onSuccess {
                logger.info("Loaded language ${choice.code}")
            }.onFailure {
                logger.error("Failed to load language ${choice.code}", it)
            }.getOrNull()
        }
    }

    fun languageChoiceFromCode(code: String): ClientLanguage? {
        if (code.isBlank()) {
            return null
        }

        return ClientLanguage.entries.firstOrNull { language ->
            language.code.equals(code, true)
        }
    }

    private fun currentLanguageChoice(): ClientLanguage {
        if (clientLanguage != ClientLanguage.AUTO) {
            return clientLanguage
        }

        val minecraftChoice = MINECRAFT_LANGUAGE
        if (minecraftChoice != null) {
            return minecraftChoice
        }

        return COMMON_UNDERSTOOD_LANGUAGE
    }

    fun loadDefault() {
        loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)
        loadLanguage(currentLanguageChoice())
    }

    fun getLanguage() = loadLanguage(currentLanguageChoice()) ?: getCommonLanguage()

    fun getCommonLanguage() = loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)

    fun hasFallbackTranslation(key: String) =
        getCommonLanguage()?.has(key) ?: false

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
