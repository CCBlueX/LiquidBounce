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
package net.ccbluex.liquidbounce.features.command.commands.module

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.MultiSelectArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.preset.pagedList
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoDisable
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.text.asText
import net.minecraft.ChatFormatting

/**
 * AutoDisable Command
 *
 * Allows you to manage the list of modules that are automatically disabled.
 * It provides subcommands to add, remove, list and clear modules from the auto-disable list.
 *
 * Module: [ModuleAutoDisable]
 */
object CommandAutoDisable : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("autodisable") {
            literal("clear") {
                exec {
                    ModuleAutoDisable.clear()
                    chat(
                        t("clear.modulesCleared"),
                        metadata = MessageMetadata(id = "CAutoDisable#global")
                    )
                    1
                }
            }
            pagedList(
                header = {
                    t("list.modules")
                        .withColor(ChatFormatting.RED)
                        .bold(true)
                },
                items = {
                    ModuleAutoDisable.modules
                },
                eachRow = { _, module ->
                    "\u2B25 ".asText()
                        .withStyle(ChatFormatting.BLUE)
                        .append(variable(module.name).copyable())
                }
            )
            literal("remove") {
                argument(
                    "modules",
                    MultiSelectArgumentType("Module", ModuleManager, predicate = { true }, nameOf = ClientModule::name),
                ) { modules ->
                    exec { ctx ->
                        removeModules(ctx.get(modules))
                        1
                    }
                }
            }
            literal("add") {
                argument(
                    "modules",
                    MultiSelectArgumentType("Module", ModuleManager, predicate = { true }, nameOf = ClientModule::name),
                ) { modules ->
                    exec { ctx ->
                        addModules(ctx.get(modules))
                        1
                    }
                }
            }
        }
    }

    private fun CmdI18n.addModules(modules: Set<ClientModule>) {
        modules.forEach { module ->
            if (!ModuleAutoDisable.add(module)) {
                throw CommandException(t("add.moduleIsPresent", module.name))
            }

            chat(
                regular(
                    t("add.moduleAdded",
                        variable(module.name)
                    )
                ),
                metadata = MessageMetadata(id = "CAutoDisable#${module.name}")
            )
        }
    }

    private fun CmdI18n.removeModules(modules: Set<ClientModule>) {
        modules.forEach { module ->
            if (!ModuleAutoDisable.remove(module)) {
                throw CommandException(t("remove.moduleNotPresent", module.name))
            }

            chat(
                regular(
                    t("remove.moduleRemoved",
                        variable(module.name)
                    )
                ),
                metadata = MessageMetadata(id = "CAutoDisable#${module.name}")
            )
        }
    }

}
