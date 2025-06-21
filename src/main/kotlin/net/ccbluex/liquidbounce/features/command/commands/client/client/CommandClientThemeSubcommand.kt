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

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util

object CommandClientThemeSubcommand {
    fun themeCommand() = CommandBuilder.begin("theme")
        .hub()
        .subcommand(listSubcommand())
        .subcommand(setSubcommand())
        .subcommand(browseSubcommand())
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler { command, _ ->
        Util.getOperatingSystem().open(ThemeManager.themesFolder)
        chat(regular("Location: "), clickablePath(ThemeManager.themesFolder))
    }.build()

    private fun setSubcommand() = CommandBuilder.begin("set")
        .parameter(
            ParameterBuilder.begin<String>("theme")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .autocompletedWith { s, _ ->
                    ThemeManager.themes().filter { it.startsWith(s, true) }
                }
                .build()
        )
        .handler { command, args ->
            val name = args[0] as String

            if (name.equals("default", true)) {
                ThemeManager.activeTheme = ThemeManager.defaultTheme
                chat(regular("Switching theme to default..."))
                return@handler
            }

            runCatching {
                ThemeManager.chooseTheme(name)
            }.onFailure {
                chat(markAsError("Failed to switch theme: ${it.message}"))
            }.onSuccess {
                chat(regular("Switched theme to $name."))
            }
        }.build()

    private fun listSubcommand() = CommandBuilder.begin("list")
        .handler { command, args ->
            @Suppress("SpreadOperator")
            (chat(
                regular("Available themes: "),
                *ThemeManager.themes().flatMapIndexed { index, name ->
                    listOf(
                        regular(if (index == 0) "" else ", "),
                        variable(name)
                    )
                }.toTypedArray()
            ))
        }.build()
}
