/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Help Command
 *
 * Provides a help page for displaying other commands.
 */
object CommandHelp : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
            .pagedQuery(
                pageSize = 8,
                header = {
                    result("help").withColor(Formatting.RED).bold(true)
                },
                items = {
                    CommandManager.sortedBy { it.name }
                },
                eachRow = { _, command ->
                    val commandStart = CommandManager.Options.prefix + command.name
                    "\u2B25 ".asText()
                        .formatted(Formatting.BLUE)
                        .onHover(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                translation("liquidbounce.command.${command.name}.description")
                            )
                        )
                        .append(
                            commandStart.asText()
                                .formatted(Formatting.GRAY)
                                .onClick {
                                    mc.setScreen(ChatScreen(commandStart))
                                }
                        )
                        .append(buildAliasesText(command))
                }
            )
    }

    private fun buildAliasesText(cmd: Command): Text {
        val aliasesText = Text.literal("")

        if (cmd.aliases.isNotEmpty()) {
            cmd.aliases.forEach { alias ->
                aliasesText += ", ".asText().formatted(Formatting.DARK_GRAY)
                aliasesText += regular(alias).formatted(Formatting.GRAY)
                    .onClick {
                        mc.setScreen(ChatScreen(CommandManager.Options.prefix + alias))
                    }
            }
        }

        return aliasesText
    }

}
