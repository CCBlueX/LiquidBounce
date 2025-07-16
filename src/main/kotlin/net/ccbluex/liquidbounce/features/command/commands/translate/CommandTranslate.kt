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

import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.api.thirdparty.translator.asLanguage
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleTranslation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

object CommandTranslate : CommandFactory {

    override fun createCommand() = CommandBuilder.begin("translate")
        .alias("tr")
        .parameter(
            ParameterBuilder.begin<String>("sourceLanguage")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { begin, _ ->
                    (listOf("auto") + languageCodes.keys).filter { it.startsWith(begin, ignoreCase = true) }
                }
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder.begin<String>("targetLanguage")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompleteWithLanguageCodes()
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder.begin<String>("text")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .vararg()
                .build()
        )
        .suspendHandler(false, ::handler)
        .build()

    private suspend fun handler(command: Command, args: Array<Any>) {
        val (sourceLanguage, targetLanguage, texts) = args
        sourceLanguage as String
        targetLanguage as String
        texts as Array<*>

        if (sourceLanguage.equals(targetLanguage, ignoreCase = true)) {
            throw CommandException(command.result("sameLanguage"))
        }

        val text = texts.joinToString(" ")
        val result = ModuleTranslation.translate(
            sourceLanguage.asLanguage(), targetLanguage.asLanguage(), text
        )

        if (result is TranslationResult.Success) {
            if (result.translation == result.origin) {
                throw CommandException(command.result("sameText"))
            } else {
                chat(
                    regular("("),
                    variable(result.fromLanguage.literal),
                    regular(") "),
                    regular(result.origin)
                        .copyable(copyContent = result.origin),
                )
                chat(
                    regular("("),
                    variable(result.toLanguage.literal),
                    regular(") "),
                    regular(result.translation)
                        .copyable(copyContent = result.translation),
                )
            }
        } else {
            chat(result.toResultText())
        }
    }
}
