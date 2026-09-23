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

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.commands.client.config.ConfigEditCommand
import net.ccbluex.liquidbounce.features.command.commands.client.config.ConfigInfoCommand
import net.ccbluex.liquidbounce.features.command.commands.client.config.ConfigListCommand
import net.ccbluex.liquidbounce.features.command.commands.client.config.ConfigLoadCommand
import net.ccbluex.liquidbounce.features.command.commands.client.config.ConfigPublishCommand
import net.ccbluex.liquidbounce.features.command.commands.client.config.ConfigReportCommand

/**
 * Config Command
 *
 * Loads, publishes and manages configs on the marketplace.
 */
object CommandConfig : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("config") {
            with(ConfigListCommand) {
                list()
                search()
                tags()
            }
            with(ConfigInfoCommand) { info() }

            with(ConfigLoadCommand) {
                load()
                revert()
                restore()
                detach()
            }
            with(ConfigReportCommand) { report() }

            with(ConfigPublishCommand) { publish() }
            with(ConfigEditCommand) { edit() }
        }
    }

}
