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

import net.ccbluex.liquidbounce.api.v1.AutoSettings
import net.ccbluex.liquidbounce.api.v1.ClientApiV1.requestSettingsList
import net.ccbluex.liquidbounce.api.v1.ClientApiV1.requestSettingsScript
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.io.HttpClient.get
import net.minecraft.text.Text

/**
 * Config Command
 *
 * Provides various subcommands related to the configuration,
 * such as loading configuration from an external source or an API
 * and listing available configurations.
 */
object CommandConfig {

    internal var loadingNow = false
    internal var cachedSettingsList: Array<AutoSettings>? = null

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
                            loadingNow = true
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
                            loadingNow = false
                            return@handler
                        }

                        // Get online config from API
                        loadingNow = true
                        runCatching {
                            requestSettingsScript(name).apply {
                                ConfigSystem.deserializeConfigurable(ModuleManager.modulesConfigurable, reader(),
                                    ConfigSystem.autoConfigGson)
                            }
                        }.onFailure {
                            chat(markAsError(command.result("failedToLoad", variable(name))))
                        }.onSuccess {
                            chat(regular(command.result("loaded", variable(name))))
                        }
                        loadingNow = false
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .handler { command, args ->
                        runCatching {
                            chat(regular(command.result("loading")))
                            (cachedSettingsList ?: requestSettingsList()).forEach {
                                chat(
                                    regular("§a${it.name}"),
                                    regular(" (id: "),
                                    variable(it.settingId),
                                    regular(", updated on ${it.dateFormatted}, status: "),
                                    Text.literal(it.statusType.displayName).styled {
                                            style -> style.withFormatting(it.statusType.formatting)
                                    },
                                    regular(")")
                                )
                            }
                        }.onFailure {
                            chat(regular("§cFailed to load settings list from API"))
                        }
                    }
                    .build()
            )
//            .subcommand(
//                CommandBuilder
//                    .begin("share")
//                    .handler { command, args ->
//                        // todo: implement share command
//                    }
//                    .build()
//            )
            .build()
    }

    fun autoComplete(begin: String, validator: (Module) -> Boolean = { true }): List<String> {
        return cachedSettingsList?.map { it.settingId }?.filter { it.startsWith(begin, true) } ?: emptyList()
    }

}
