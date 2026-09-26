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

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.PlayerInfoArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.world

/**
 * RemoteView Command
 *
 * Allows you to view from the perspective of another player in the game.
 */
object CommandRemoteView : CommandRegistrar {
    private var pName: String? = null

    @Suppress("detekt:LongMethod", "detekt:CognitiveComplexMethod")
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("remoteview", aliases = listOf("rv")) {
            requires { it.isIngame }

            literal("view") {
                argument("name", PlayerInfoArgumentType) { name ->
                    exec { ctx ->
                        val playerName = ctx.get(name).profile.name
                        for (entity in world.players()) {
                            if (playerName.equals(entity.scoreboardName, true)) {
                                if (mc.cameraEntity === entity) {
                                    chat(
                                        regular(t("view.alreadyViewing", variable(entity.scoreboardName))),
                                        metadata = MessageMetadata(id = "CRemoteView#info")
                                    )
                                    return@exec 1
                                }

                                mc.cameraEntity = entity
                                pName = entity.scoreboardName
                                chat(
                                    regular(t("view.viewPlayer", variable(entity.scoreboardName))),
                                    metadata = MessageMetadata(id = "CRemoteView#info")
                                )
                                chat(
                                    regular(t("view.caseOff", variable(entity.scoreboardName))),
                                    metadata = MessageMetadata(id = "CRemoteView#info", remove = false)
                                )

                                break
                            }
                        }
                        1
                    }
                }
            }
            literal("off") {
                exec {
                    if (mc.cameraEntity != player) {
                        mc.cameraEntity = player
                        chat(
                            regular(t("off.off", variable(pName.toString()))),
                            metadata = MessageMetadata(id = "CRemoteView#info")
                        )
                        pName = null
                    } else {
                        chat(
                            regular(t("off.alreadyOff")),
                            metadata = MessageMetadata(id = "CRemoteView#info")
                        )
                    }
                    1
                }
            }
        }
    }

}
