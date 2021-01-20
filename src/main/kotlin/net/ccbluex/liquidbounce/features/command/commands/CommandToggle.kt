/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.defaultColor
import net.ccbluex.liquidbounce.utils.statusColor
import net.ccbluex.liquidbounce.utils.variableColor

object CommandToggle {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("toggle")
            .alias("t")
            .description("Allows you to toggle modules")
            .parameter(ParameterBuilder
                .begin<String>("name")
                .description("The name of the module")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build())
            .handler { args ->
                val name = args[0] as String
                val module = LiquidBounce.moduleManager.find { it.name.equals(name, true) } ?: throw CommandException("Module §b§l${args[1]}§c not found.")

                module.state = !module.state
                chat("$variableColor${module.name} ${defaultColor}has been $statusColor${if (module.state) "enabled" else "disabled"}$variableColor.")
            }
            .build()
    }

}
