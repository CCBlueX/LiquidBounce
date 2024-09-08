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

import net.ccbluex.liquidbounce.config.AutoConfig.loadingNow
import net.ccbluex.liquidbounce.config.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.IncludeConfiguration
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util

/**
 * LocalConfig Command
 *
 * Allows you to load, list, and create local configurations.
 */
@IncludeCommand
object CommandLocalConfig : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("localconfig")
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

                        ConfigSystem.userConfigsFolder.resolve("$name.json").runCatching {
                            if (!exists()) {
                                chat(regular(command.result("notFound", variable(name))))
                                return@handler
                            }

                            loadingNow = true
                            ConfigSystem.deserializeConfigurable(ModuleManager.modulesConfigurable, reader(),
                                ConfigSystem.autoConfigGson)
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
                    .parameter(
                        ParameterBuilder
                            .begin<String>("online")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        chat("§cSettings:")
                        for (files in ConfigSystem.userConfigsFolder.listFiles()!!) {
                            chat(regular(files.name))
                        }
                    }
                    .build()
            )
            .subcommand(CommandBuilder.begin("browse").handler { command, _ ->
                Util.getOperatingSystem().open(ConfigSystem.userConfigsFolder)
                chat(regular(command.result("browse", variable(ConfigSystem.userConfigsFolder.absolutePath))))
            }.build())
            .subcommand(
                CommandBuilder
                    .begin("save")
                    .alias("create")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder
                            .begin<String>("include")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .autocompletedWith { s ->
                                listOf(
                                    "binds",
                                    "hidden"
                                ).filter { it.startsWith(s) }
                            }
                            .vararg()
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        @Suppress("UNCHECKED_CAST")
                        val include = args.getOrNull(1) as Array<*>? ?: emptyArray<String>()

                        val includeConfiguration = IncludeConfiguration(
                            includeBinds = include.contains("binds"),
                            includeHidden = include.contains("hidden")
                        )

                        ConfigSystem.userConfigsFolder.resolve("$name.json").runCatching {
                            if (exists()) {
                                delete()
                            }

                            createNewFile()
                            serializeAutoConfig(writer(), includeConfiguration)
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

    private fun autoComplete(begin: String, validator: (Module) -> Boolean = { true }): List<String> {
        return ConfigSystem.userConfigsFolder.listFiles()?.map { it.nameWithoutExtension }
            ?.filter { it.startsWith(begin) } ?: emptyList()
    }

}
