/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

object CommandToggle {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("toggle")
            .alias("t")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .autocompletedWith(ModuleManager::autoComplete)
                    .required()
                    .build()
            )
            .handler { command, args ->
                val name = args[0] as String
                val module = ModuleManager.find { it.name.equals(name, true) }
                    ?: throw CommandException(command.result("moduleNotFound", name))

                val newState = !module.enabled
                module.enabled = newState
                chat(regular(command.result("moduleToggled", variable(module.name), variable(if (newState) command.result("enabled") else command.result("disabled")))))
            }
            .build()
    }

}
