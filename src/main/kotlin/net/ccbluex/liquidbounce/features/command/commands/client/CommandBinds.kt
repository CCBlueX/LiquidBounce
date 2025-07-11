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
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.input.availableInputKeys
import net.ccbluex.liquidbounce.utils.input.inputByName
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Formatting

/**
 * Binds Command
 *
 * Allows you to manage the bindings of modules to keys.
 * It provides subcommands to add, remove, list and clear bindings.
 */
object CommandBinds : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("binds")
            .hub()
            .subcommand(addSubcommand())
            .subcommand(removeSubcommand())
            .subcommand(listSubcommand())
            .subcommand(clearSubcommand())
            .build()
    }

    private fun clearSubcommand() = CommandBuilder
        .begin("clear")
        .handler { command, _ ->
            ModuleManager.forEach { it.bind.unbind() }
            chat(command.result("bindsCleared"), metadata = MessageMetadata(id = "Binds#global"))
        }
        .build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .pagedQuery(
            pageSize = 8,
            header = {
                result("bindings").withColor(Formatting.RED).bold(true)
            },
            items = {
                ModuleManager.filter { !it.bind.isUnbound }
            },
            eachRow = { _, module ->
                "\u2B25 ".asText()
                    .formatted(Formatting.BLUE)
                    .append(variable(module.name).copyable())
                    .append(regular(": "))
                    .append(regular(module.bind.keyName).copyable())
                    .append(regular("("))
                    .append(variable(module.bind.action.choiceName))
                    .append(regular(")"))
            }
        )


    private fun removeSubcommand() = CommandBuilder
        .begin("remove")
        .parameter(
            Parameters.modules { mod -> !mod.bind.isUnbound }
                .required()
                .build()
        )
        .handler { command, args ->
            val modules = args[0] as Set<ClientModule>

            modules.forEach { module ->
                if (module.bind.isUnbound) {
                    throw CommandException(command.result("moduleNotBound"))
                }

                module.bind.unbind()

                chat(
                    regular(command.result("bindRemoved", variable(module.name))),
                    metadata = MessageMetadata(id = "Binds#${module.name}")
                )
            }

            ModuleClickGui.reload()
        }
        .build()

    private fun addSubcommand() = CommandBuilder
        .begin("add")
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

            val bindKey = inputByName(keyName)
            if (bindKey == InputUtil.UNKNOWN_KEY) {
                throw CommandException(command.result("unknownKey"))
            }

            module.bind.bind(bindKey)
            ModuleClickGui.reload()
            chat(
                regular(
                    command.result(
                        "moduleBound", variable(module.name),
                        variable(module.bind.keyName)
                    )
                ), metadata = MessageMetadata(id = "Binds#${module.name}")
            )
        }
        .build()

}
