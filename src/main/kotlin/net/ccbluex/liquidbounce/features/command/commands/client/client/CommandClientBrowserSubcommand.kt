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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.integration.BrowserScreen
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientBrowserSubcommand {
    fun browserCommand() = CommandBuilder.begin("browser")
        .hub()
        .subcommand(openSubcommand())
        .build()

    private fun openSubcommand() = CommandBuilder.begin("open")
        .parameter(
            ParameterBuilder.begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .build()
        ).handler { command, args ->
            chat(regular("Opening browser..."))
            RenderSystem.recordRenderCall {
                mc.setScreen(BrowserScreen(args[0] as String))
            }
        }.build()
}
