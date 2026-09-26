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

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.MultiSelectArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.preset.pagedList
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.ccbluex.liquidbounce.utils.text.joinToText
import net.minecraft.ChatFormatting

/**
 * Hide Command
 *
 * Allows you to hide specific modules.
 */
object CommandHide : CommandRegistrar {
    @Suppress("detekt:LongMethod")
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("hide") {
            literal("hide") {
                argument(
                    "modules",
                    MultiSelectArgumentType(
                        "Module",
                        ModuleManager,
                        predicate = { !it.hidden },
                        nameOf = ClientModule::name
                    ),
                ) { modules ->
                    exec { ctx ->
                        val hiddenModules = ctx.get(modules)
                        hiddenModules.forEach { it.hidden = true }

                        chat(
                            t("hide.moduleHidden",
                                hiddenModules.map { variable(it.name) }.joinToText(", ".asPlainText())
                            ),
                            metadata = MessageMetadata(id = "CHide#info")
                        )
                        1
                    }
                }
            }
            literal("unhide") {
                argument(
                    "modules",
                    MultiSelectArgumentType(
                        "Module",
                        ModuleManager,
                        predicate = { it.hidden },
                        nameOf = ClientModule::name
                    ),
                ) { modules ->
                    exec { ctx ->
                        val unhiddenModules = ctx.get(modules)
                        unhiddenModules.forEach { it.hidden = false }

                        chat(
                            t("unhide.moduleUnhidden",
                                unhiddenModules.map { variable(it.name) }.joinToText(", ".asPlainText())
                            ),
                            metadata = MessageMetadata(id = "CHide#info")
                        )
                        1
                    }
                }
            }
            literal("clear") {
                exec {
                    ModuleManager.forEach { it.hidden = false }
                    chat(
                        regular(
                            t("clear.modulesUnhidden")
                        ),
                        metadata = MessageMetadata(id = "CHide#info")
                    )
                    1
                }
            }
            pagedList(
                header = {
                    t("list.hidden")
                        .withColor(ChatFormatting.RED)
                        .bold(true)
                },
                items = {
                    ModuleManager.filter { it.hidden }
                },
                eachRow = { _, module ->
                    "\u2B25 ".asText()
                        .withStyle(ChatFormatting.BLUE)
                        .append(variable(module.name).copyable())
                        .append(regular(" ("))
                        .append(regular(t("list.hidden"))) // TODO: click to unhide?
                        .append(regular(")"))
                }
            )
        }
    }

}
