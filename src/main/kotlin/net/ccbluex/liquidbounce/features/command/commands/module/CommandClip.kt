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
package net.ccbluex.liquidbounce.features.command.commands.module

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Clips
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Clip Command
 *
 * Saves the last 20 seconds and the next 5 of the game as a video, while GameBot records clips.
 *
 * Module: [net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot]
 */
object CommandClip : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("clip") {
            optional("label", ClientStringArgumentType.word(), default = "manual") { label ->
                exec { ctx ->
                    val name = ctx.get(label).replace(UNSAFE, "_").take(MAX_LABEL).ifEmpty { "manual" }
                    if (Clips.save(name)) {
                        chat(regular(t("saving", variable(name))))
                    } else {
                        chat(markAsError(t("off")))
                    }
                    1
                }
            }
        }
    }

    private val UNSAFE = Regex("[^A-Za-z0-9_-]")
    private const val MAX_LABEL = 40

}
