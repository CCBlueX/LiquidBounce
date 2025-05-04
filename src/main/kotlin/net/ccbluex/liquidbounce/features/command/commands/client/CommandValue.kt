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
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

/**
 * Value Command
 *
 * Allows you to set/reset the value of a specific module.
 */
object CommandValue : CommandFactory {

    @Suppress("SwallowedException", "LongMethod", "CognitiveComplexMethod")
    override fun createCommand(): Command {
        return CommandBuilder
            .begin("value")
            .parameter(
                ParameterBuilder
                    .begin<ClientModule>("moduleName")
                    .verifiedBy(ParameterBuilder.MODULE_VALIDATOR)
                    .autocompletedWith { begin, _ -> ModuleManager.autoComplete(begin) }
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("valueName/resetAllValues")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .autocompletedWith { begin, args ->
                        val module = ModuleManager.find { it.name.equals(args[1], true) }
                        if (module == null) return@autocompletedWith listOf("resetAllValues")
                        val values = module.getContainedValuesRecursively()
                            .filter { !it.name.equals("Bind", true) }
                            .map { it.name } + "resetAllValues"
                        values.filter { it.startsWith(begin, true) }
                    }
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("value/reset")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .autocompletedWith { begin, args ->
                        val moduleName = args.getOrNull(1) ?: return@autocompletedWith emptyList()
                        val module = ModuleManager.find {
                            it.name.equals(moduleName, true)
                        } ?: return@autocompletedWith emptyList()

                        val valueName = args.getOrNull(2) ?: return@autocompletedWith emptyList()
                        if (valueName.equals("resetAllValues", true)) return@autocompletedWith emptyList()

                        val value = module.getContainedValuesRecursively().firstOrNull {
                            it.name.equals(valueName, true)
                        } ?: return@autocompletedWith emptyList()

                        val options = value.valueType.completer.possible(value) + "reset"
                        options.filter { it.startsWith(begin, true) }
                    }
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val module = args[0] as ClientModule
                val valueName = args[1] as String

                if (valueName.equals("resetAllValues", true)) {
                    module.getContainedValuesRecursively()
                        .filter { !it.name.equals("Bind", true) }
                        .forEach { it.restore() }
                    ModuleClickGui.reloadView()
                    chat(
                        regular(command.result("resetAllSuccess", module.name)),
                        metadata = MessageMetadata(id = "CValue#resetAll${module.name}")
                    )
                    return@handler
                }

                val valueOrReset = args.getOrNull(2) as? String
                    ?: throw CommandException(command.result("valueNotFound", valueName))

                val value = module.getContainedValuesRecursively()
                    .filter { !it.name.equals("Bind", true) }
                    .firstOrNull { it.name.equals(valueName, true) }
                    ?: throw CommandException(command.result("valueNotFound", valueName))

                if (valueOrReset.equals("reset", true)) {
                    value.restore()
                    ModuleClickGui.reloadView()
                    chat(
                        regular(command.result("resetSuccess", valueName, module.name)),
                        metadata = MessageMetadata(id = "CValue#reset${module.name}")
                    )
                } else {
                    try {
                        value.setByString(valueOrReset)
                        ModuleClickGui.reloadView()
                    } catch (e: Exception) {
                        throw CommandException(command.result("valueError", valueName, e.message ?: ""))
                    }

                    chat(
                        regular(command.result("success")),
                        metadata = MessageMetadata(id = "CValue#success${module.name}")
                    )
                }
            }
            .build()
    }

}
