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
package net.ccbluex.liquidbounce.features.command.commands.translate

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat

object CommandAutoTranslate : ValueGroup("AutoTranslate"), CommandRegistrar {
    var languageCode by text("LanguageCode", "en")
        private set

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("autotranslate") {
            literal("language") {
                exec {
                    chat(
                        t("language.code",
                            languageCode,
                            languageCodes[languageCode]?.displayName ?: "Unknown"
                        ),
                        metadata = MessageMetadata(id = "Clanguage#info")
                    )
                    1
                }
                literal("set") {
                    argument(
                        "languageCode",
                        ClientStringArgumentType.word(),
                        suggestions(strings = { languageCodes.keys }),
                    ) { codeArg ->
                        exec { ctx ->
                            val code = ctx.get(codeArg)
                            val name = languageCodes[code]?.displayName
                                ?: throw CommandException(
                                    t("language.set.unrecognized",
                                        code
                                    )
                                )
                            languageCode = code
                            chat(
                                t("language.set.set",
                                    code,
                                    name
                                ),
                                metadata = MessageMetadata(id = "Cset#info")
                            )
                            1
                        }
                    }
                }
            }
        }
    }

}
