/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.lang

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.text.*
import net.minecraft.util.Language
import java.util.*

fun translation(key: String, vararg args: Any): MutableText =
    MutableText.of(LanguageText(key, args))

object LanguageManager : Configurable("lang") {

    // Current language
    private val language: String
        get() = overrideLanguage.ifBlank { mc.options.language }

    // The game language can be overridden by the user
    var overrideLanguage by text("OverrideLanguage", "")

    // Common language
    private const val COMMON_UNDERSTOOD_LANGUAGE = "en_us"

    // List of all languages
    val knownLanguages = arrayOf(
        "en_us",
        "de_de",
        "ja_jp",
        "zh_cn",
        "ru_ru",
        "ua_ua",
        "en_pt",
        "pt_br"
    )
    private val languageMap = mutableMapOf<String, ClientLanguage>()

    /**
     * Load all languages which are pre-defined in [knownLanguages] and stored in assets.
     * If a language is not found, it will be logged as error.
     *
     * Languages are stored in assets/minecraft/liquidbounce/lang and when loaded will be stored in [languageMap]
     */
    fun loadLanguages() {
        for (language in knownLanguages) {
            runCatching {
                val languageFile = javaClass.getResourceAsStream("/assets/liquidbounce/lang/$language.json")
                val translations = decode<HashMap<String, String>>(languageFile.reader().readText())

                languageMap[language] = ClientLanguage(translations)
            }.onSuccess {
                logger.info("Loaded language $language")
            }.onFailure {
                logger.error("Failed to load language $language", it)
            }
        }
    }

    fun getLanguage() = languageMap[language] ?: languageMap[COMMON_UNDERSTOOD_LANGUAGE]

    fun getCommonLanguage() = languageMap[COMMON_UNDERSTOOD_LANGUAGE]

    fun hasFallbackTranslation(key: String) =
        languageMap[COMMON_UNDERSTOOD_LANGUAGE]?.hasTranslation(key) ?: false

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
    override fun get(key: String, fallback: String?) = getTranslation(key)
        ?: LanguageManager.getCommonLanguage()?.getTranslation(key)
        ?: fallback
        ?: key

    override fun hasTranslation(key: String) = translations.containsKey(key)

    override fun isRightToLeft() = false

    override fun reorder(text: StringVisitable) = OrderedText { visitor ->
        text.visit({ style, string ->
            if (TextVisitFactory.visitFormatted(string, style, visitor)) {
                Optional.empty()
            } else {
                StringVisitable.TERMINATE_VISIT
            }
        }, Style.EMPTY).isPresent
    }

}
