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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.enumChoice
import net.ccbluex.liquidbounce.features.command.builder.module
import net.ccbluex.liquidbounce.features.command.builder.modules
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.onClickRun
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.input.availableInputKeys
import net.ccbluex.liquidbounce.utils.input.bind
import net.ccbluex.liquidbounce.utils.input.inputByName
import net.ccbluex.liquidbounce.utils.input.renderText
import net.ccbluex.liquidbounce.utils.input.unbind
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.HoverEvent

/**
 * Binds Command
 *
 * Allows you to manage the bindings of modules to keys.
 * It provides subcommands to add, remove, list and clear bindings.
 */
object CommandBinds : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("binds")
            .hub()
            .subcommand(addSubcommand)
            .subcommand(removeSubcommand)
            .subcommand(listSubcommand)
            .subcommand(clearSubcommand)
            .build()
    }

    private val clearSubcommand = CommandBuilder
        .begin("clear")
        .handler {
            ModuleManager.forEach { it.bindValue.unbind() }
            chat(command.result("bindsCleared"), metadata = MessageMetadata(id = "Binds#global"))
        }
        .build()

    private val listSubcommand = CommandBuilder
        .begin("list")
        .pagedQuery(
            pageSize = 8,
            header = {
                result("bindings").withColor(ChatFormatting.RED).bold(true)
            },
            items = {
                ModuleManager.filter { !it.bind.isUnbound }
            },
            eachRow = { _, module ->
                val bind = module.bind
                "\u2B25 ".asText()
                    .withStyle(ChatFormatting.BLUE)
                    .append(
                        markAsError("[\u2715] ")
                            .onHover(
                                HoverEvent.ShowText(
                                    "Unbind ".asText().append(variable(module.name))
                                )
                            )
                            .onClickRun {
                                runCatching {
                                    handleRemoveBind(setOf(module))
                                }.onFailure(CommandExecutor::handleExceptions)
                            }
                    )
                    .append(highlight(module.name).copyable())
                    .append(regular(": "))
                    .append(bind.renderText())
            }
        )

    private fun handleRemoveBind(modules: Set<ClientModule>) {
        modules.forEach { module ->
            if (module.bind.isUnbound) {
                throw CommandException(removeSubcommand.result("moduleNotBound"))
            }

            module.bindValue.unbind()

            chat(
                regular(removeSubcommand.result("bindRemoved", variable(module.name))),
                metadata = MessageMetadata(id = "Binds#${module.name}")
            )
        }

        ModuleClickGui.sync()
    }

    private val removeSubcommand = CommandBuilder
        .begin("remove")
        .parameter(
            ParameterBuilder.modules { mod -> !mod.bind.isUnbound }
                .required()
                .build()
        )
        .handler {
            val modules = args[0] as Set<ClientModule>
            handleRemoveBind(modules)
        }
        .build()

    private val addSubcommand = CommandBuilder
        .begin("add")
        .parameter(
            ParameterBuilder.module()
                .required()
                .build()
        ).parameter(
            ParameterBuilder
                .begin<String>("key")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedFrom { availableInputKeys }
                .required()
                .build()
        ).parameter(
            ParameterBuilder.enumChoice<InputBind.BindAction>("action")
                .optional()
                .build()
        )
        .parameter(
            ParameterBuilder.enumChoice<InputBind.Modifier>("modifiers")
                .vararg()
                .optional()
                .build()
        )
        .handler {
            val module = args[0] as ClientModule
            val keyName = args[1] as String
            val action = args.getOrNull(2) as InputBind.BindAction? ?: module.bind.action
            val modifiers = args.getOrNull(3) as Set<InputBind.Modifier>? ?: module.bind.modifiers

            val bindKey = inputByName(keyName)
            if (bindKey == InputConstants.UNKNOWN) {
                throw CommandException(command.result("unknownKey"))
            }

            module.bindValue.bind(bindKey, action, modifiers)
            ModuleClickGui.sync()
            chat(
                regular(
                    command.result("moduleBound", variable(module.name), module.bind.renderText())
                ), metadata = MessageMetadata(id = "Binds#${module.name}")
            )
        }
        .build()

}
