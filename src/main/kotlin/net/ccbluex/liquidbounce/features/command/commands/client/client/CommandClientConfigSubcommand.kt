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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.configs
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfig
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util
import java.time.LocalDateTime

/**
 * Configurable Management Command
 *
 * Allows you to back up, restore, reset, and browse configurations.
 */
object CommandClientConfigSubcommand {

    fun configCommand() = CommandBuilder.begin("config")
        .hub()
        .subcommand(backupSubcommand())
        .subcommand(restoreSubcommand())
        .subcommand(resetSubCommand())
        .subcommand(browseSubcommand())
        .build()

    private val defaultConfigs
        get() = listOf(
            modulesConfig
        )

    private fun backupSubcommand() = CommandBuilder.begin("backup")
        .parameter(
            ParameterBuilder.configs()
                .optional()
                .build()
        )
        .handler {
            val configs = args.getOrNull(0) as Set<Config>? ?: defaultConfigs
            val formattedNames = configs.joinToString(", ") { config ->
                config.name.toLowerCamelCase()
            }

            runCatching {
                chat(regular(command.result("backingUp", variable(formattedNames))))
                for (config in configs) {
                    ConfigSystem.store(config)
                }

                val fileName = "manual-${LocalDateTime.now().toUnderlinedString()}"
                ConfigSystem.backup(fileName, configs)
                fileName
            }.onFailure { exception ->
                chat(markAsError(command.result("failedToBackup", exception.message ?: "Unknown error")))
            }.onSuccess { fileName ->
                chat(regular(command.result("backedUp", variable(fileName))))
                chat(regular(command.result("restoreHelp", variable(".client config restore $fileName"))))
            }
        }.build()

    private fun restoreSubcommand() = CommandBuilder.begin("restore")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedFrom {
                    ConfigSystem.backupFolder.listFiles()
                        ?.map { file -> file.nameWithoutExtension }
                }
                .required()
                .build()
        )
        .handler {
            val fileName = args[0] as String

            AutoConfig.withLoading {
                runCatching {
                    chat(regular(command.result("restoring", variable(fileName))))

                    ConfigSystem.restore(fileName)
                }.onFailure { exception ->
                    chat(markAsError(command.result("failedToRestore", exception.message ?: "Unknown error")))
                }.onSuccess {
                    chat(regular(command.result("restored", variable(fileName))))
                }
            }
        }.build()

    @Suppress("CognitiveComplexMethod")
    private fun resetSubCommand() = CommandBuilder
        .begin("reset")
        .parameter(
            ParameterBuilder.configs()
                .optional()
                .build()
        )
        .handler {
            val configs = args.getOrNull(0) as Set<Config>? ?: defaultConfigs
            val formattedNames = configs.joinToString(", ") { config ->
                config.name.toLowerCamelCase()
            }

            AutoConfig.withLoading {
                runCatching {
                    chat(regular(command.result("resetting", variable(formattedNames))))

                    for (config in configs) {
                        // TODO: We could straight up use configurable.restore(), however, we
                        //   want to filter out the ModuleHud module

                        for (value in config.inner) {
                            // TODO: Remove when HUD no longer contains the Element Configuration
                            if (value is ModuleHud) {
                                continue
                            }

                            value.restore()
                        }
                    }
                }.onSuccess {
                    chat(regular(command.result("reset")))
                }.onFailure { exception ->
                    chat(markAsError(command.result("failedToReset", exception.message ?: "Unknown error")))
                }
            }
        }
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler {
        Util.getPlatform().openFile(ConfigSystem.backupFolder)
        chat(regular(command.result("browse", variable(ConfigSystem.backupFolder.absolutePath))))
    }.build()

}
