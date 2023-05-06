/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

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

                        // TODO: Add online configs

                        ConfigSystem.userConfigsFolder.resolve("$name.json").runCatching {
                            if (!exists()) {
                                chat(regular(command.result("notFound", variable(name))))
                                return@handler
                            }

                            ConfigSystem.deserializeConfigurable(ModuleManager.modulesConfigurable, reader())
                        }.onFailure {
                            chat(regular(command.result("failedToLoad", variable(name))))
                        }.onSuccess {
                            chat(regular(command.result("loaded", variable(name))))
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("create")
                    .alias("new", "save", "store")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .autocompletedWith(this::autoComplete)
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder
                            .begin<Boolean>("overwrite")
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val overwrite = (args.getOrNull(1) as? String ?: "false").equals("true", true)

                        ConfigSystem.userConfigsFolder.resolve("$name.json").runCatching {
                            if (exists()) {
                                if (!overwrite) {
                                    chat(regular(command.result("alreadyExists", variable(name))))
                                    return@handler
                                } else {
                                    delete()
                                }
                            }

                            if (!exists()) {
                                createNewFile()
                            }

                            // TODO: Fix module states being stored

                            // Store the config
                            ConfigSystem.serializeConfigurable(ModuleManager.modulesConfigurable, writer())
                        }.onFailure {
                            chat(regular(command.result("failedToCreate", variable(name))))
                        }.onSuccess {
                            chat(regular(command.result("created", variable(name))))
                        }
                    }
                    .build()
            )
            .build()
    }

    fun autoComplete(begin: String, validator: (Module) -> Boolean = { true }): List<String> {
        return emptyList()
    }

}
