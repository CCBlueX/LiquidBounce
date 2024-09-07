/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

/**
 * Value Command
 *
 * Allows you to set the value of a specific module.
 */
@IncludeCommand
object CommandValue : CommandFactory {

    override fun createCommand(): Command {
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
                    .autocompletedWith { begin, args ->
                        val module = ModuleManager.find { it.name.equals(args[1], true) }
                        if (module == null) return@autocompletedWith emptyList()

                        module.getContainedValuesRecursively()
                            .filter { it.name.startsWith(begin, true) }
                            .map { it.name }
                    }
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("value")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .useMinecraftAutoCompletion()
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
                    throw CommandException(command.result("valueError", valueName, e.message ?: ""))
                }

                chat(regular(command.result("success")))
            }
            .build()
    }
}
