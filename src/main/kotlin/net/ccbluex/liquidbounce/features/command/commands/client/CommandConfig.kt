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

import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.config.AutoConfig.configsCache
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.moduleParameter
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.io.HttpClient.get
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import kotlin.concurrent.thread

/**
 * Config Command
 *
 * Provides various subcommands related to the configuration,
 * such as loading configuration from an external source or an API
 * and listing available configurations.
 */
@IncludeCommand
object CommandConfig {

    private const val CONFIGS_URL = "https://github.com/CCBlueX/LiquidCloud/tree/main/LiquidBounce/settings/nextgen"

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
                    .parameter(
                        moduleParameter()
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val moduleNames = args.getOrNull(1) as String?
                        val modules = ModuleManager.parseModulesFromParameter(moduleNames)

                        // Load the config in a separate thread to prevent the client from freezing
                        thread(name = "config-loader") {
                            runCatching {
                                if(name.startsWith("http")) {
                                    // Load the config from the specified URL
                                    get(name).reader()
                                } else {
                                    // Get online config from API
                                    ClientApi.requestSettingsScript(name).reader()
                                }
                            }.onSuccess { sourceReader ->
                                AutoConfig.loadingNow = true
                                runCatching {
                                    sourceReader.apply {
                                        if(modules.isEmpty()) {
                                            ConfigSystem.deserializeConfigurable(
                                                ModuleManager.modulesConfigurable, this,
                                                ConfigSystem.autoConfigGson
                                            )
                                        } else {
                                            ConfigSystem.deserializeModuleConfigurable(
                                                modules, this,
                                                ConfigSystem.autoConfigGson
                                            )
                                        }
                                    }
                                }.onFailure {
                                    chat(markAsError(command.result("failedToLoad", variable(name))))
                                }.onSuccess {
                                    chat(regular(command.result("loaded", variable(name))))
                                }
                                AutoConfig.loadingNow = false
                            }.onFailure {
                                chat(markAsError(command.result("failedToLoad", variable(name))))
                            }
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
                                val spaces = " ".repeat(
                                    (width - mc.textRenderer.getWidth(settingName))
                                        / widthOfSpace
                                )

                                chat(
                                    variable(settingName).styled { style ->
                                        style
                                            .withClickEvent(
                                                ClickEvent(
                                                    ClickEvent.Action.SUGGEST_COMMAND,
                                                    ".config load $settingName"
                                                )
                                            )
                                            .withHoverEvent(
                                                HoverEvent(
                                                    HoverEvent.Action.SHOW_TEXT,
                                                    Text.of("§7Click to load $settingName")
                                                )
                                            )
                                    },
                                    regular(spaces),
                                    regular(" | "),
                                    variable(it.dateFormatted),
                                    regular(" | "),
                                    Text.literal(it.statusType.displayName).styled { style ->
                                        style
                                            .withFormatting(it.statusType.formatting)
                                            .withHoverEvent(
                                                HoverEvent(
                                                    HoverEvent.Action.SHOW_TEXT,
                                                    Text.of(it.statusDateFormatted)
                                                )
                                            )
                                    },
                                    regular(" | ${it.serverAddress ?: "Global"}"), prefix = false
                                )
                            }
                        }.onFailure {
                            chat(regular("§cFailed to load settings list from API"))
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("browse")
                    .handler { _, _ ->
                        browseUrl(CONFIGS_URL)
                    }
                    .build()
            )
            .build()
    }

    fun autoComplete(begin: String, validator: (Module) -> Boolean = { true }): List<String> {
        return configsCache?.map { it.settingId }?.filter { it.startsWith(begin, true) } ?: emptyList()
    }


}
