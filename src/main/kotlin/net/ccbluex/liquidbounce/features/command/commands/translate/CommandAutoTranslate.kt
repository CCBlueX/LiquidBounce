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
package net.ccbluex.liquidbounce.features.command.commands.translate

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat

object CommandAutoTranslate : Configurable("AutoTranslate"), CommandFactory {

    init {
        ConfigSystem.root(this)
    }

    var languageCode by text("LanguageCode", "en")
        private set

    override fun createCommand() = CommandBuilder.begin("autotranslate")
        .hub()
        .subcommand(languageCommand())
        .build()

    private fun languageCommand() = CommandBuilder.begin("language")
        .handler { command, _ ->
            chat(command.result("code", languageCode, languageCodes[languageCode]?.displayName ?: "Unknown"), command)
        }
        .subcommand(setLanguageCommand())
        .build()

    private fun setLanguageCommand() = CommandBuilder.begin("set")
        .parameter(
            ParameterBuilder.begin<String>("languageCode")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompleteWithLanguageCodes()
                .required()
                .build()
        )
        .handler { command, args ->
            val code = args[0] as String
            val name = languageCodes[code]?.displayName ?: throw CommandException(command.result("unrecognized", code))
            languageCode = code
            chat(command.result("set", code, name), command)
        }
        .build()

}
