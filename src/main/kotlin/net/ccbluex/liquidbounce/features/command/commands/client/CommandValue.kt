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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.valueGroupKeyPath
import net.ccbluex.liquidbounce.features.command.builder.valueKeyPath
import net.ccbluex.liquidbounce.features.command.builder.valueType
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Value Command
 *
 * Allows you to change values by key path.
 */
@Suppress("SwallowedException")
object CommandValue : Command.Factory {

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
            ParameterBuilder.valueKeyPath("path")
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder.valueType()
                .required()
                .build()
        )
        .handler {
            val valueKey = args[0] as String
            val valueString = args[1] as String

            val value = ConfigSystem.findValueByKey(valueKey)
                ?: throw CommandException(command.result("valueNotFound", valueKey))

            try {
                value.setByString(valueString)
                ModuleClickGui.sync()
            } catch (e: Exception) {
                throw CommandException(command.result("valueError", valueKey, e.message ?: ""))
            }

            chat(
                regular(command.result("success", variable(valueKey))),
                metadata = MessageMetadata(id = "CValue#success${valueKey}")
            )
        }
        .build()

    private fun resetSubCommand() = CommandBuilder
        .begin("reset")
        .parameter(
            ParameterBuilder.valueKeyPath("path")
                .required()
                .build()
        )
        .handler {
            val valueKey = args[0] as String

            val value = ConfigSystem.findValueByKey(valueKey)
                ?: throw CommandException(command.result("valueNotFound", valueKey))

            value.restore()
            ModuleClickGui.sync()
            chat(
                regular(command.result("resetSuccess", variable(valueKey))),
                metadata = MessageMetadata(id = "CValue#reset${valueKey}")
            )
        }
        .build()

    private fun resetAllSubCommand() = CommandBuilder
        .begin("reset-all")
        .parameter(
            ParameterBuilder.valueGroupKeyPath("valueGroupPath")
                .required()
                .build()
        )
        .handler {
            val valueGroupKey = args[0] as String
            val valueGroup = ConfigSystem.findValueGroupByKey(valueGroupKey)
                ?: throw CommandException(command.result("valueGroupNotFound", valueGroupKey))

            valueGroup.collectValuesRecursively()
                .filter { !it.name.equals("Bind", true) }
                .forEach { it.restore() }
            ModuleClickGui.sync()
            chat(
                regular(command.result("resetAllSuccess", variable(valueGroupKey))),
                metadata = MessageMetadata(id = "CValue#resetAll${valueGroupKey}")
            )
        }
        .build()

}
