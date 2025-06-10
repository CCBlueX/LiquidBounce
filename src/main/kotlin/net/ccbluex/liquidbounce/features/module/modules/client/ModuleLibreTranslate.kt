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
package net.ccbluex.liquidbounce.features.module.modules.client

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.thirdparty.LIBRE_TRANSLATE_BASE_URL
import net.ccbluex.liquidbounce.api.thirdparty.LibreTranslateApi
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import kotlin.time.Duration.Companion.seconds

object ModuleLibreTranslate : ClientModule(
    "LibreTranslate",
    Category.CLIENT,
    hide = true,
    state = false,
    aliases = arrayOf("Translate", "Translator")
) {

    private val apiBaseUrl by text("ApiBaseUrl", default = LIBRE_TRANSLATE_BASE_URL)
        .doNotIncludeAlways()
        .also { value ->
            withScope {
                value.asFlow().debounce(1.seconds).collect { url ->
                    val client = LibreTranslateApi(url.trimEnd('/'))
                    try {
                        val languages = client.languages()
                        this@ModuleLibreTranslate.client = client
                        notification(
                            title = "API loaded",
                            message = "${languages.size} languages available on base URL $url",
                            NotificationEvent.Severity.SUCCESS
                        )
                    } catch (e: Exception) {
                        notification(
                            title = "Invalid API",
                            message = "Failed to fetch languages from base URL $url!",
                            NotificationEvent.Severity.ERROR
                        )
                        logger.error("Failed to fetch languages", e)
                    }
                }
            }
        }

    private val apiKey by text("ApiKey", default = "")
        .doNotIncludeAlways()
        .onChange(String::trim)

    private val nullableApiKey get() = apiKey.takeIf { it.isNotEmpty() }

    private val targetLanguage: String by text("TargetLanguage", default = "")
        .doNotIncludeAlways()
        .onChange(String::trim)
        .also { value ->
            withScope {
                value.asFlow().filter { it.isNotBlank() }.debounce(1.seconds).collect { lang ->
                    val client = client ?: return@collect
                    val languages = client.languages()
                    val language = languages.find { it.code == lang }
                    if (language == null) {
                        notification("Invalid Language", "Language '$lang' is invalid.", NotificationEvent.Severity.ERROR)
                    } else {
                        notification(
                            title = "Valid Language",
                            message = "Language ${language.readableString()} can be translated from: " +
                                languages.filter { lang in it.targets }.joinToString { it.readableString() },
                            NotificationEvent.Severity.SUCCESS
                        )
                    }
                }
            }
        }

    fun LibreTranslateApi.Language.readableString() = "'$name'($code)"

    private var showSourceLanguage by boolean("ShowSourceLanguage", default = true).doNotIncludeAlways()

    private val autoTranslate by multiEnumChoice("AutoTranslate", default = Translatable.entries)
    private enum class Translatable(override val choiceName: String) : NamedChoice {
        CHAT_MESSAGES("ChatMessages"),
        SUBTITLES("Subtitles"),
        LIQUID_CHAT_MESSAGES("LiquidChatMessages"),
    }


    @Volatile
    private var client: LibreTranslateApi? = null

    // TODO: LiquidChat handler: pub/pri msg

    // TODO: Chat handler: player/server msg


}
