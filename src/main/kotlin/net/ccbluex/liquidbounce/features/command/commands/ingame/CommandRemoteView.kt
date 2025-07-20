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
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * RemoteView Command
 *
 * Allows you to view from the perspective of another player in the game.
 */
object CommandRemoteView : CommandFactory, MinecraftShortcuts {

    private var pName: String? = null

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("remoteview")
            .alias("rv")
            .hub()
            .requiresIngame()
            .subcommand(offSubcommand())
            .subcommand(viewSubcommand())
            .build()
    }

    private fun viewSubcommand() = CommandBuilder
        .begin("view")
        .parameter(
            Parameters.playerName()
                .required()
                .build()
        )
        .handler { command, args ->
            val name = args[0] as String
            for (entity in mc.world!!.entities) {
                if (name.equals(entity.nameForScoreboard, true)) {
                    if (mc.getCameraEntity() == entity) {
                        chat(
                            regular(command.result("alreadyViewing", variable(entity.nameForScoreboard))),
                            metadata = MessageMetadata(id = "CRemoteView#info")
                        )
                        return@handler
                    }

                    mc.setCameraEntity(entity)
                    pName = entity.nameForScoreboard
                    chat(
                        regular(command.result("viewPlayer", variable(entity.nameForScoreboard))),
                        metadata = MessageMetadata(id = "CRemoteView#info")
                    )
                    chat(
                        regular(command.result("caseOff", variable(entity.nameForScoreboard))),
                        metadata = MessageMetadata(id = "CRemoteView#info", remove = false)
                    )

                    break
                }
            }
        }
        .build()

    private fun offSubcommand() = CommandBuilder
        .begin("off")
        .handler { command, _ ->
            if (mc.getCameraEntity() != player) {
                mc.setCameraEntity(player)
                chat(
                    regular(command.result("off", variable(pName.toString()))),
                    metadata = MessageMetadata(id = "CRemoteView#info")
                )
                pName = null
            } else {
                chat(
                    regular(command.result("alreadyOff")),
                    metadata = MessageMetadata(id = "CRemoteView#info")
                )
            }
        }
        .build()

}
