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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientLanguageChangedEvent
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.client.NullableBypass
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@AddonApi
fun translation(key: String, vararg args: Any?): MutableComponent =
    MutableComponent.create(LanguageText(key, args))

/**
 * Translations are loaded on the render thread from what is bundled or cached; downloads run off it in [select].
 */
object LanguageManager : ValueGroup("Language") {

    var clientLanguage by enumChoice("ClientLanguage", ClientLanguage.AUTO)
        .onChanged { _ -> withScope { select(currentLanguageCode()) } }

    private const val COMMON_UNDERSTOOD_LANGUAGE = "en_us"

    enum class ClientLanguage(override val tag: String, val code: String? = null) : Tagged {
        AUTO("Auto"),
        EN_US("English (US)", "en_us"),
        EN_PT("English (Pirate)", "en_pt"),
        DE_DE("German", "de_de"),
        JA_JP("Japanese", "ja_jp"),
        ZH_CN("Chinese (Simplified)", "zh_cn"),
        ZH_TW("Chinese (Traditional)", "zh_tw"),
        RU_RU("Russian", "ru_ru"),
        UK_UA("Ukrainian", "uk_ua"),
        PT_BR("Portuguese (Brazil)", "pt_br"),
        TR_TR("Turkish", "tr_tr"),
        NL_NL("Dutch (Netherlands)", "nl_nl"),
        NL_BE("Dutch (Belgium)", "nl_be"),
    }

    val languageCodes: Set<String>
        get() = ClientLanguage.entries.mapNotNull(ClientLanguage::code).toSet() + PublishedLanguages.codes

    private val languageRegistry = ConcurrentHashMap<String, net.ccbluex.liquidbounce.lang.ClientLanguage>()

    fun interface TranslationSource {
        fun load(code: String): Map<String, String>?
    }

    private val clientTranslations = TranslationSource { code ->
        javaClass.getResourceAsStream("/resources/liquidbounce/lang/$code.json")
            ?.readJson<HashMap<String, String>>()
    }

    // Published first, so a fix made after this release beats the bundled file.
    private val sources = CopyOnWriteArrayList(listOf(PublishedLanguages, clientTranslations))

    private val selection = Mutex()

    /**
     * The language [select] last made ready, which [ClientLanguageChangedEvent] announced.
     */
    @Volatile
    private var selectedCode: String? = null

    /**
     * Earlier sources win, so an add-on cannot override a built-in or published key.
     */
    fun registerSource(source: TranslationSource) {
        sources += source
        languageRegistry.clear()
    }

    private fun loadLanguage(code: String) = languageRegistry.computeIfAbsent(code, ::createLanguage)

    private fun createLanguage(code: String): net.ccbluex.liquidbounce.lang.ClientLanguage {
        val translations = mergedTranslations(code)
        logger.info("Loaded language $code (${translations.size} keys)")
        return ClientLanguage(translations)
    }

    private fun mergedTranslations(code: String): Map<String, String> {
        val merged = HashMap<String, String>()

        for (source in sources) {
            val translations = runCatching { source.load(code) }
                .onFailure { logger.error("Translation source failed for $code", it) }
                .getOrNull() ?: continue

            for ((key, value) in translations) {
                merged.putIfAbsent(key, value)
            }
        }

        return merged
    }

    /**
     * Downloads [code] when the published file changed, then announces it once it is loaded.
     */
    private suspend fun select(code: String) = selection.withLock {
        val isUpdated = PublishedLanguages.download(code)
        if (isUpdated) {
            languageRegistry[code] = createLanguage(code)
            languageRegistry.keys.retainAll(setOf(COMMON_UNDERSTOOD_LANGUAGE, code))
        } else {
            loadLanguage(code)
        }

        if (isUpdated || code != selectedCode) {
            selectedCode = code
            EventManager.callEvent(ClientLanguageChangedEvent())
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

    /**
     * Minecraft's language under [ClientLanguage.AUTO] also covers languages published after this release.
     */
    private fun currentLanguageCode(): String {
        clientLanguage.code?.let { return it }

        val code = NullableBypass.mc()?.options?.languageCode ?: return COMMON_UNDERSTOOD_LANGUAGE
        return code.takeIf { languageChoiceFromCode(it) != null || PublishedLanguages.has(it) }
            ?: COMMON_UNDERSTOOD_LANGUAGE
    }

    suspend fun loadDefault() {
        loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)
        select(currentLanguageCode())
    }

    fun getLanguage() = loadLanguage(currentLanguageCode())

    fun getCommonLanguage() = loadLanguage(COMMON_UNDERSTOOD_LANGUAGE)

    fun hasFallbackTranslation(key: String) = getCommonLanguage().has(key)

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
        ?: LanguageManager.getCommonLanguage().getTranslation(key)
        ?: fallback

    override fun has(key: String) = translations.containsKey(key)

    override fun isDefaultRightToLeft() = false

    override fun getVisualOrder(text: FormattedText) = Language.DEFAULT_INSTANCE.getVisualOrder(text)

}
