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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Toggle Command
 *
 * Allows you to enable or disable a specific module.
 */
object CommandToggle : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("toggle")
            .alias("t")
            .parameter(
                Parameters.module()
                    .required()
                    .build()
            )
            .handler { command, args ->
                val module = args[0] as ClientModule

                val newState = !module.running
                module.enabled = newState
                chat(
                    regular(
                        command.result(
                            "moduleToggled",
                            variable(module.name),
                            variable(if (newState) command.result("enabled") else command.result("disabled"))
                        )
                    ),
                    metadata = MessageMetadata(id = "CToggle#success${module.name}")
                )
            }
            .build()
    }

}
