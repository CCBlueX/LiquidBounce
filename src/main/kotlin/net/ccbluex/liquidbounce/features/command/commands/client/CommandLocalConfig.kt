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

import net.ccbluex.liquidbounce.api.models.client.AutoSettings
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.IncludeConfiguration
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.modules
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import java.time.Instant
import java.time.ZoneId

/**
 * LocalConfig Command
 *
 * Allows you to load, list, and create local configurations.
 */
object CommandLocalConfig : Command.Factory {

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
                .autocompletedFrom { listOf("binds", "hidden") }
                .vararg()
                .optional()
                .build()
        )
        .handler {
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
                bufferedWriter().use {
                    serializeAutoConfig(it, includeConfiguration)
                }
            }.onFailure {
                chat(regular(command.result("failedToCreate", variable(name))))
                logger.error("Failed to create local config '$name'", it)
            }.onSuccess {
                chat(regular(command.result("created", variable(name))))
            }
        }
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler {
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
        .handler {
            val configFiles = ConfigSystem.userConfigsFolder.listFiles { file, name ->
                name.endsWith(".json", ignoreCase = true)
            }

            if (configFiles.isNullOrEmpty()) {
                chat("No local config!".asText().formatted(Formatting.RED))
            } else {
                chat("Settings:".asText().formatted(Formatting.AQUA))
                for (file in configFiles) {
                    val fileNameWithoutSuffix = file.name.removeSuffix(".json")

                    chat(
                        variable(file.name)
                            .onClick(
                                ClickEvent(
                                    ClickEvent.Action.SUGGEST_COMMAND,
                                    ".localconfig load $fileNameWithoutSuffix"
                                )
                            )
                            .onHover(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Text.of("§7Click to load ${file.name}")
                                )
                            ),
                        regular(" ("),
                        regular(
                            Instant.ofEpochMilli(file.lastModified())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(AutoSettings.FORMATTER)
                        ),
                        regular(")"),
                    )
                }
            }
        }
        .build()

    private fun loadSubcommand() = CommandBuilder
        .begin("load")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedFrom {
                    ConfigSystem.userConfigsFolder.listFiles()?.map { it.nameWithoutExtension }
                }
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder.modules()
                .optional()
                .build()
        )
        .handler {
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

}
