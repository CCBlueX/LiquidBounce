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

import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Value Command
 *
 * Allows you to change values of a specific module.
 */
@Suppress("SwallowedException")
object CommandValue : CommandFactory {

    override fun createCommand() = CommandBuilder
        .begin("value")
        .hub()
        .subcommand(setSubCommand())
        .subcommand(resetSubCommand())
        .subcommand(resetAllSubCommand())
        .build()

    private fun setSubCommand() = CommandBuilder
        .begin("set")
        .parameter(
            Parameters.module("moduleName")
                .required()
                .build()
        )
        .parameter(
            Parameters.valueName()
                .required()
                .build()
        )
        .parameter(
            Parameters.valueType()
                .required()
                .build()
        )
        .handler { command, args ->
            val module = args[0] as ClientModule
            val valueName = args[1] as String
            val valueString = args[2] as String

            val value = module.getContainedValuesRecursively()
                .filter { !it.name.equals("Bind", true) }
                .firstOrNull { it.name.equals(valueName, true) }
                ?: throw CommandException(command.result("valueNotFound", valueName))

            try {
                value.setByString(valueString)
                ModuleClickGui.reload()
            } catch (e: Exception) {
                throw CommandException(command.result("valueError", valueName, e.message ?: ""))
            }

            chat(
                regular(command.result("success", variable(valueName), variable(module.name))),
                metadata = MessageMetadata(id = "CValue#success${module.name}")
            )
        }
        .build()

    private fun resetSubCommand() = CommandBuilder
        .begin("reset")
        .parameter(
            Parameters.module("moduleName")
                .required()
                .build()
        )
        .parameter(
            Parameters.valueName()
                .required()
                .build()
        )
        .handler { command, args ->
            val module = args[0] as ClientModule
            val valueName = args[1] as String

            val value = module.getContainedValuesRecursively()
                .filter { !it.name.equals("Bind", true) }
                .firstOrNull { it.name.equals(valueName, true) }
                ?: throw CommandException(command.result("valueNotFound", valueName))

            value.restore()
            ModuleClickGui.reload()
            chat(
                regular(command.result("resetSuccess", variable(valueName), variable(module.name))),
                metadata = MessageMetadata(id = "CValue#reset${module.name}")
            )
        }
        .build()

    private fun resetAllSubCommand() = CommandBuilder
        .begin("reset-all")
        .parameter(
            Parameters.module("moduleName")
                .required()
                .build()
        )
        .handler { command, args ->
            val module = args[0] as ClientModule

            module.getContainedValuesRecursively()
                .filter { !it.name.equals("Bind", true) }
                .forEach { it.restore() }
            ModuleClickGui.reload()
            chat(
                regular(command.result("resetAllSuccess", variable(module.name))),
                metadata = MessageMetadata(id = "CValue#resetAll${module.name}")
            )
        }
        .build()

}
