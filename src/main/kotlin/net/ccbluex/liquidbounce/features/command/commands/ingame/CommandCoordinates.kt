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
package net.ccbluex.liquidbounce.features.command.commands.ingame

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.playerName
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.ChatFormatting
import org.apache.commons.lang3.StringUtils

/**
 * Coordinates Command
 *
 * Copies your coordinates to your clipboard.
 */
object CommandCoordinates : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("coordinates")
            .alias("position", "coords")
            .hub()
            .requiresIngame()
            .subcommand(
                CommandBuilder.begin("whisper")
                    .parameter(
                        ParameterBuilder.playerName()
                            .required()
                            .build()
                    )
                    .handler {
                        val name = args[0] as String
                        network.sendChat("/msg $name ${getCoordinates(fancy = true)}")
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("copy")
                    .handler {
                        mc.keyboardHandler.clipboard = getCoordinates()
                        chat(command.result("success"), command)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("info")
                    .handler {
                        chat(getCoordinates().asPlainText(ChatFormatting.GRAY), command)
                    }
                    .build()
            )
            .build()
    }

    private fun getCoordinates(fancy: Boolean = false): String {
        val pos = player.blockPosition()
        val dimension = StringUtils.capitalize(world.dimension().identifier().path)
        val start = if (fancy) "My coordinates are: " else ""
        return start +
            "x: ${pos.x}, y: ${pos.y}, z: ${pos.z} " +
            "in the $dimension"
    }

}
