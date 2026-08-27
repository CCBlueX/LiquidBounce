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
import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.ModuleArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.MultiSelectArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.TaggedArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.command.preset.pagedList
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
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
import net.ccbluex.liquidbounce.utils.text.asText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.HoverEvent

/**
 * Binds Command
 *
 * Allows you to manage the bindings of modules to keys.
 * It provides subcommands to add, remove, list and clear bindings.
 */
object CommandBinds : CommandRegistrar {
    @Suppress("detekt:LongMethod")
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("binds") binds@{
            literal("clear") {
                exec {
                    ModuleManager.forEach { it.bindValue.unbind() }
                    chat(
                        t("clear.bindsCleared"),
                        metadata = MessageMetadata(id = "Binds#global")
                    )
                    1
                }
            }
            pagedList(
                header = {
                    t("list.bindings")
                        .withColor(ChatFormatting.RED)
                        .bold(true)
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
                                        this@binds.handleRemoveBind(setOf(module))
                                    }.onFailure(CommandExecutor::handleExceptions)
                                }
                        )
                        .append(highlight(module.name).copyable())
                        .append(regular(": "))
                        .append(bind.renderText())
                }
            )
            literal("remove") {
                argument(
                    "modules",
                    MultiSelectArgumentType(
                        "Module",
                        ModuleManager,
                        predicate = { !it.bind.isUnbound },
                        nameOf = ClientModule::name,
                    ),
                ) { modules ->
                    exec { ctx ->
                        handleRemoveBind(ctx.get(modules))
                        1
                    }
                }
            }
            literal("add") {
                argument("module", ModuleArgumentType("module")) { module ->
                    argument("key", ClientStringArgumentType.word(), suggestions(availableInputKeys)) { key ->
                        optional(
                            "action",
                            TaggedArgumentType<InputBind.BindAction>("action"),
                            default = null,
                        ) { action ->
                            optional(
                                "modifiers",
                                MultiSelectArgumentType(
                                    "Modifier",
                                    InputBind.Modifier.entries,
                                    predicate = { true },
                                    nameOf = InputBind.Modifier::tag,
                                ),
                                default = null,
                            ) { modifiers ->
                                exec { ctx ->
                                    addBind(
                                        ctx.get(module),
                                        ctx.get(key),
                                        ctx.get(action),
                                        ctx.get(modifiers),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun CmdI18n.addBind(
        module: ClientModule,
        keyName: String,
        action: InputBind.BindAction?,
        modifiers: Set<InputBind.Modifier>?,
    ): Int {
        val resolvedAction = action ?: module.bind.action
        val resolvedModifiers = modifiers ?: module.bind.modifiers

        val bindKey = inputByName(keyName)
        if (bindKey == InputConstants.UNKNOWN) {
            throw CommandException(t("add.unknownKey"))
        }

        module.bindValue.bind(bindKey, resolvedAction, resolvedModifiers)
        ModuleClickGui.sync()
        chat(
            regular(
                t("add.moduleBound",
                    variable(module.name),
                    module.bind.renderText()
                )
            ),
            metadata = MessageMetadata(id = "Binds#${module.name}")
        )

        return 1
    }

    private fun CmdI18n.handleRemoveBind(modules: Set<ClientModule>) {
        modules.forEach { module ->
            if (module.bind.isUnbound) {
                throw CommandException(t("remove.moduleNotBound"))
            }

            module.bindValue.unbind()

            chat(
                regular(
                    t("remove.bindRemoved",
                        variable(module.name)
                    )
                ),
                metadata = MessageMetadata(id = "Binds#${module.name}")
            )
        }

        ModuleClickGui.sync()
    }

}
