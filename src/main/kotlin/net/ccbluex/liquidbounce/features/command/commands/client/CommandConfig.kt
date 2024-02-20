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

import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.config.AutoConfig.configsCache
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.io.HttpClient.get
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text

/**
 * Config Command
 *
 * Provides various subcommands related to the configuration,
 * such as loading configuration from an external source or an API
 * and listing available configurations.
 */
object CommandConfig {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("config")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("load")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .autocompletedWith(this::autoComplete)
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String

                        // Get online config from external source
                        if (name.startsWith("http")) {
                            AutoConfig.loadingNow = true
                            runCatching {
                                get(name).apply {
                                    ConfigSystem.deserializeConfigurable(ModuleManager.modulesConfigurable, reader(),
                                        ConfigSystem.autoConfigGson)
                                }
                            }.onFailure {
                                chat(markAsError(command.result("failedToLoad", variable(name))))
                            }.onSuccess {
                                chat(regular(command.result("loaded", variable(name))))
                            }
                            AutoConfig.loadingNow = false
                            return@handler
                        }


                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .handler { command, args ->
                        runCatching {
                            chat(regular(command.result("loading")))
                            val widthOfSpace = mc.textRenderer.getWidth(" ")
                            val width = configs.maxOf { mc.textRenderer.getWidth(it.settingId) }

                            // In case of the chat, we want to show the newest config at the bottom for visibility
                            configs.sortedBy { it.javaDate.time }.forEach {
                                val settingName = it.settingId // there is also .name, but we use it for GUI instead

                                // Append spaces to the setting name to align the date and status
                                // Compensate for the length of the setting name
                                val spaces = " ".repeat((width - mc.textRenderer.getWidth(settingName))
                                    / widthOfSpace)

                                chat(
                                    variable(settingName).styled {
                                        style -> style
                                            .withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                                ".config load $settingName"))
                                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Text.of("§7Click to load $settingName")))
                                    },
                                    regular(spaces),
                                    regular(" | "),
                                    variable(it.dateFormatted),
                                    regular(" | "),
                                    Text.literal(it.statusType.displayName).styled {
                                        style -> style
                                            .withFormatting(it.statusType.formatting)
                                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Text.of(it.statusDateFormatted)))
                                    },
                                    regular(" | ${it.serverAddress ?: "Global"}")
                                , prefix = false)
                            }
                        }.onFailure {
                            chat(regular("§cFailed to load settings list from API"))
                        }
                    }
                    .build()
            )
            .build()
    }

    fun autoComplete(begin: String, validator: (Module) -> Boolean = { true }): List<String> {
        return configsCache?.map { it.settingId }?.filter { it.startsWith(begin, true) } ?: emptyList()
    }



}
