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
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientAppearanceSubcommand {
    fun appearanceCommand() = CommandBuilder.begin("appearance")
        .hub()
        .subcommand(hideSubcommand())
        .subcommand(showSubcommand())
        .build()

    private fun showSubcommand() = CommandBuilder.begin("show")
        .handler { command, _ ->
            if (!HideAppearance.isHidingNow) {
                chat(regular(command.result("alreadyShowingAppearance")))
                return@handler
            }

            chat(regular(command.result("showingAppearance")))
            HideAppearance.isHidingNow = false
        }.build()

    private fun hideSubcommand() = CommandBuilder.begin("hide")
        .handler { command, _ ->
            if (HideAppearance.isHidingNow) {
                chat(regular(command.result("alreadyHidingAppearance")))
                return@handler
            }

            chat(regular(command.result("hidingAppearance")))
            HideAppearance.isHidingNow = true
        }.build()
}
