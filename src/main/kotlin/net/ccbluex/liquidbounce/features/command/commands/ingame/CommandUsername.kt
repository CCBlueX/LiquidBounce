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
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.bypassNameProtection
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.italic
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.underline
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * CommandUsername
 *
 * Displays the current username.
 */
object CommandUsername : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("username") {
            requires { it.isIngame }
            exec {
                val username = it.source.playerOrNull!!.name.string
                val formattedUsernameWithEvents = variable(username)
                    .bypassNameProtection()
                    .copyable(copyContent = username)
                    .italic(true)
                    .underline(true)

                chat(
                    regular(t("username", formattedUsernameWithEvents)),
                    metadata = MessageMetadata(id = "Cusername#info")
                )
                mc.keyboardHandler.clipboard = username
                1
            }
        }
    }

}
