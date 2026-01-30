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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.plusAssign
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.withColor
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

/**
 * Help Command
 *
 * Provides a help page for displaying other commands.
 */
object CommandHelp : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
            .pagedQuery(
                pageSize = 8,
                header = {
                    result("help").withColor(ChatFormatting.RED).bold(true)
                },
                items = {
                    CommandManager.sortedBy { it.name }
                },
                eachRow = { _, command ->
                    val commandStart = CommandManager.GlobalSettings.prefix + command.name
                    "\u2B25 ".asText()
                        .withStyle(ChatFormatting.BLUE)
                        .onHover(
                            HoverEvent.ShowText(
                                translation("liquidbounce.command.${command.name}.description")
                            )
                        )
                        .append(
                            commandStart.asText()
                                .withStyle(ChatFormatting.GRAY)
                                .onClick(ClickEvent.SuggestCommand(commandStart))
                        )
                        .append(buildAliasesText(command))
                }
            )
    }

    private fun buildAliasesText(cmd: Command): Component {
        val aliasesText = Component.literal("")

        if (cmd.aliases.isNotEmpty()) {
            cmd.aliases.forEach { alias ->
                aliasesText += ", ".asPlainText(ChatFormatting.DARK_GRAY)
                aliasesText += regular(alias).withStyle(ChatFormatting.GRAY)
                    .onClick(ClickEvent.SuggestCommand(CommandManager.GlobalSettings.prefix + alias))
            }
        }

        return aliasesText
    }

}
