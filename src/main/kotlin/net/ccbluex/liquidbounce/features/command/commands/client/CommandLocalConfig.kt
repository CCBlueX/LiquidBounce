/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.config.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.IncludeConfiguration
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.util.Util

/**
 * LocalConfig Command
 *
 * Allows you to load, list, and create local configurations.
 */
object CommandLocalConfig : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("localconfig")
            .hub()
            .subcommand(loadSubcommand())
            .subcommand(listSubcommand())
            .subcommand(browseSubcommand())
            .subcommand(saveSubcommand())
            .build()
    }

    private fun saveSubcommand() = CommandBuilder
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
                .autocompletedWith { s, _ ->
                    arrayOf("binds", "hidden").filter { it.startsWith(s) }
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
                serializeAutoConfig(bufferedWriter(), includeConfiguration)
            }.onFailure {
                chat(regular(command.result("failedToCreate", variable(name))))
            }.onSuccess {
                chat(regular(command.result("created", variable(name))))
            }
        }
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler { command, _ ->
        Util.getOperatingSystem().open(ConfigSystem.userConfigsFolder)
        chat(regular(command.result("browse", clickablePath(ConfigSystem.userConfigsFolder))))
    }.build()

    private fun listSubcommand() = CommandBuilder
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

    private fun loadSubcommand() = CommandBuilder
        .begin("load")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { begin, _ -> this.autoComplete(begin) }
                .required()
                .build()
        )
        .parameter(
            Parameters.modules()
                .optional()
                .build()
        )
        .handler { command, args ->
            val name = args[0] as String
            val modules = args.getOrNull(1) as Set<ClientModule>? ?: emptySet()

            ConfigSystem.userConfigsFolder.resolve("$name.json").runCatching {
                if (!exists()) {
                    chat(regular(command.result("notFound", variable(name))))
                    return@handler
                }

                bufferedReader().use { r ->
                    AutoConfig.withLoading {
                        AutoConfig.loadAutoConfig(r, modules)
                    }
                }
            }.onFailure { error ->
                logger.error("Failed to load config $name", error)
                chat(markAsError(command.result("failedToLoad", variable(name))))
            }.onSuccess {
                chat(regular(command.result("loaded", variable(name))))
            }
        }
        .build()

    private fun autoComplete(begin: String): List<String> {
        return ConfigSystem.userConfigsFolder.listFiles()?.map { it.nameWithoutExtension }
            ?.filter { it.startsWith(begin) } ?: emptyList()
    }

}
