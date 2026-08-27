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
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.network.chat.MutableComponent

/**
 * Panic Command
 *
 * Allows you to disable all modules or modules in a specific category.
 */
object CommandPanic : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("panic") {
            exec {
                // Omitting the category disables all non-render modules.
                panic("nonrender")
            }
            argument("category", ClientStringArgumentType.word()) { category ->
                exec { ctx ->
                    panic(ctx.get(category))
                }
            }
        }
    }

    private fun CmdI18n.panic(type: String): Int {
        var modules = ModuleManager.filter { it.running }
        val msg: MutableComponent = when (type) {
            "all" -> t("disabledAllModules")
            "nonrender" -> {
                modules = modules.filter {
                    it.category != ModuleCategories.RENDER
                }
                t("disabledAllCategoryModules",
                    t("nonRender")
                )
            }

            else -> {
                val category = ModuleCategories.byName(type)
                    ?: throw CommandException(t("categoryNotFound", type))
                modules = modules.filter { it.category == category }
                t("disabledAllCategoryModules", category.tag)
            }
        }

        runCatching {
            AutoConfig.withLoading {
                for (module in modules) {
                    module.enabled = false
                }
            }
        }.onSuccess {
            chat(regular(msg), metadata = MessageMetadata(id = "Cpanic#info"))
        }.onFailure {
            throw CommandException(t("panicFailed"))
        }

        return 1
    }

}
