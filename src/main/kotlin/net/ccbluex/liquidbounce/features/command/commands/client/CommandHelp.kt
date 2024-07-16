/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.pageParameter
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Help Command
 *
 * Provides a help page for displaying other commands.
 */
object CommandHelp {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
            .parameter(
                pageParameter()
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val page = if (args.size > 1) {
                    args[0] as Int
                } else {
                    1
                }.coerceAtLeast(1)

                val commands = CommandManager.sortedBy { it.name }

                // Max page
                val maxPage = ceil(commands.size / 8.0).roundToInt()
                if (page > maxPage) {
                    throw CommandException(command.result("pageNumberTooLarge", maxPage))
                }

                printMessage(command, page, maxPage, commands)
            }
            .build()
    }

    /**
     * Prints the help page.
     */
    private fun printMessage(
        command: Command,
        page: Int,
        maxPage: Int,
        commands: List<Command>
    ) {
        printHeader(command)
        printPageCount(command, page, maxPage)

        mc.inGameHud.chatHud.removeMessage("CommandHelp#Info")

        val iterPage = 8 * page
        val commandsToShow = commands.subList(iterPage - 8, iterPage.coerceAtMost(commands.size))
        commandsToShow.forEach { cmd ->
            val aliasesText = buildAliasesText(cmd)
            printCommandHelp(CommandManager.Options.prefix, cmd, aliasesText)
        }

        printNavigation(command, page, maxPage, commands)
    }

    private fun printHeader(command: Command) {
        chat(
            command.result("help").styled { it.withColor(Formatting.RED).withBold(true) },
            metadata = MessageMetadata(id = "CommandHelp#Help")
        )
    }

    private fun printPageCount(command: Command, page: Int, maxPage: Int) {
        chat(
            regular(command.result("pageCount", variable("$page / $maxPage"))),
            metadata = MessageMetadata(id = "CommandHelp#PageCount")
        )
    }

    private fun buildAliasesText(cmd: Command): Text {
        val aliasesText = Text.literal("")

        if (cmd.aliases.isNotEmpty()) {
            cmd.aliases.forEach { alias ->
                aliasesText
                    .append(variable(", "))
                    .append(
                        regular(alias)
                            .styled { it.withColor(Formatting.GRAY) }
                            .styled {
                                it.withClickEvent(RunnableClickEvent {
                                    mc.run { mc.setScreen(ChatScreen(CommandManager.Options.prefix + alias)) }
                                })
                            }
                    )
            }
        }

        return aliasesText
    }

    private fun printCommandHelp(prefix: String, cmd: Command, aliasesText: Text) {
        val commandStart = prefix + cmd.name
        chat(
            "- ".asText()
                .styled { it.withColor(Formatting.BLUE) }
                .styled {
                    it.withHoverEvent(
                        HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            translation("liquidbounce.command.${cmd.name}.description")
                        )
                    )
                }
                .append(
                    commandStart.asText()
                        .styled { it.withColor(Formatting.GRAY) }
                        .styled {
                            it.withClickEvent(RunnableClickEvent {
                                mc.run { mc.setScreen(ChatScreen(commandStart)) }
                            })
                        }
                )
                .append(aliasesText),
            metadata = MessageMetadata(id = "CommandHelp#Info", remove = false)
        )
    }

    private fun printNavigation(command: Command, page: Int, maxPage: Int, commands: List<Command>) {
        val nextPage = (page % maxPage) + 1
        val previousPage = if (page - 1 < 1) maxPage else page - 1
        chat(
            "".asText()
                .styled { it.withColor(Formatting.GRAY) }
                .append("<--".asText()
                    .styled {
                        it.withClickEvent(RunnableClickEvent {
                            printMessage(
                                command,
                                previousPage,
                                maxPage,
                                commands
                            )
                        })
                    }
                    .styled {
                        it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, command.result("previous")))
                    }
                )
                .append("[$page]")
                .append("-->".asText()
                    .styled {
                        it.withClickEvent(RunnableClickEvent { printMessage(command, nextPage, maxPage, commands) })
                    }
                    .styled {
                        it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, command.result("next")))
                    }
                ),
            metadata = MessageMetadata(id = "CommandHelp#Next")
        )
    }

}
