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
package net.ccbluex.liquidbounce.features.command.commands.ingame

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.util.Formatting
import org.apache.commons.lang3.StringUtils

/**
 * Coordinates Command
 *
 * Copies your coordinates to your clipboard.
 */
object CommandCoordinates : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("coordinates")
            .alias("position", "coords")
            .hub()
            .requiresIngame()
            .subcommand(
                CommandBuilder.begin("whisper")
                    .parameter(
                        Parameters.playerName()
                            .required()
                            .build()
                    )
                    .handler { _, args ->
                        val name = args[0] as String
                        network.sendChatMessage("/msg $name ${getCoordinates(fancy = true)}")
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("copy")
                    .handler { command, _ ->
                        mc.keyboard.clipboard = getCoordinates()
                        chat(command.result("success"), command)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("info")
                    .handler { command, _ ->
                        chat(getCoordinates().asText().styled { it.withColor(Formatting.GRAY) }, command)
                    }
                    .build()
            )
            .build()
    }

    private fun getCoordinates(fancy: Boolean = false): String {
        val pos = player.blockPos
        val dimension = StringUtils.capitalize(world.registryKey.value.path)
        val start = if (fancy) "My coordinates are: " else ""
        return start +
            "x: ${pos.x}, y: ${pos.y}, z: ${pos.z} " +
            "in the $dimension"
    }

}
