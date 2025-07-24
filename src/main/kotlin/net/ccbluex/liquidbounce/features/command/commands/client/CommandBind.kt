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
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.input.availableInputKeys

/**
 * Bind Command
 *
 * Allows you to bind a key to a module, which means that the module will be activated when the key is pressed.
 */
object CommandBind : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("bind")
            .parameter(
                Parameters.module()
                    .required()
                    .build()
            ).parameter(
                ParameterBuilder
                    .begin<String>("key")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .autocompletedWith { begin, _ -> availableInputKeys.filter { it.startsWith(begin) } }
                    .required()
                    .build()
            )
            .handler { command, args ->
                val module = args[0] as ClientModule
                val keyName = args[1] as String

                if (keyName.equals("none", true)) {
                    module.bind.unbind()
                    ModuleClickGui.reload()
                    chat(
                        regular(command.result("moduleUnbound", variable(module.name))),
                        metadata = MessageMetadata(id = "Bind#${module.name}")
                    )
                    return@handler
                }

                runCatching {
                    module.bind.bind(keyName)
                    ModuleClickGui.reload()
                }.onSuccess {
                    chat(
                        regular(command.result("moduleBound", variable(module.name), variable(module.bind.keyName))),
                        metadata = MessageMetadata(id = "Bind#${module.name}")
                    )
                }.onFailure {
                    chat(
                        markAsError(command.result("keyNotFound", variable(keyName))),
                        metadata = MessageMetadata(id = "Bind#${module.name}")
                    )
                }

            }
            .build()
    }

}
