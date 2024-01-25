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
import net.minecraft.text.MutableText

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
        "en_pt",
        "pt_br"
    )
    private val languageMap = mutableMapOf<String, Language>()

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

                languageMap[language] = Language(translations)
            }.onSuccess {
                logger.info("Loaded language $language")
            }.onFailure {
                logger.error("Failed to load language $language", it)
            }
        }
    }

    fun getTranslation(key: String): String {
        return languageMap[language]?.getTranslation(key)
            ?: languageMap[COMMON_UNDERSTOOD_LANGUAGE]?.getTranslation(key)
            ?: key
    }

    fun hasFallbackTranslation(key: String): Boolean {
        return languageMap[COMMON_UNDERSTOOD_LANGUAGE]?.getTranslation(key) != null
    }

}

data class Language(val translations: Map<String, String>) {
    fun getTranslation(key: String) = translations[key]
}
