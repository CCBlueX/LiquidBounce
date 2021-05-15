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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandValue {
    fun createCommand(): Command {
        return CommandBuilder
            .begin("value")
            .parameter(
                ParameterBuilder
                    .begin<Module>("moduleName")
                    .verifiedBy(ParameterBuilder.MODULE_VALIDATOR)
                    .autocompletedWith(ModuleManager::autoComplete)
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("valueName")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("value")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler { command, args ->
                val module = args[0] as Module
                val valueName = args[1] as String
                val valueString = args[2] as String

                val value = module.getContainedValuesRecursively()
                    .firstOrNull { it.name.equals(valueName, true) }
                    ?: throw CommandException(command.result("valueNotFound", valueName))

                try {
                    value.setByString(valueString)
                } catch (e: Exception) {
                    throw CommandException(command.result("valueError", e.message ?: ""))
                }

                chat(regular(command.result("success")))
            }
            .build()
    }
}
