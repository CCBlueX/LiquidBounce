/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.kotlin.mapArray

object CommandClientLanguageSubcommand {
    fun languageCommand() = CommandBuilder.begin("language")
        .hub()
        .subcommand(listSubcommand())
        .subcommand(setSubcommand())
        .subcommand(unsetSubcommand())
        .build()

    private fun unsetSubcommand() = CommandBuilder.begin("unset")
        .handler { command, args ->
            chat(regular("Unset override language..."))
            LanguageManager.overrideLanguage = ""
            ConfigSystem.storeConfigurable(LanguageManager)
        }.build()

    private fun setSubcommand() = CommandBuilder.begin("set")
        .parameter(
            ParameterBuilder.begin<String>("language")
                .autocompletedWith { begin, _ ->
                    LanguageManager.knownLanguages.filter { it.startsWith(begin, true) }
                }
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .build()
        ).handler { command, args ->
            val language = LanguageManager.knownLanguages.find { it.equals(args[0] as String, true) }
            if (language == null) {
                chat(regular("Language not found."))
                return@handler
            }

            chat(regular("Setting language to ${language}..."))
            LanguageManager.overrideLanguage = language

            ConfigSystem.storeConfigurable(LanguageManager)
        }.build()

    private fun listSubcommand() = CommandBuilder.begin("list")
        .handler { command, args ->
            chat(regular("Available languages:"))
            chat(texts = LanguageManager.knownLanguages.mapArray { regular("-> $it") })
        }.build()
}
