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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.util.Util
import java.time.LocalDateTime

/**
 * Configurable Management Command
 *
 * Allows you to backup, restore, reset, and browse configurations.
 */
object CommandClientConfigSubcommand {

    fun configCommand() = CommandBuilder.begin("config")
        .hub()
        .subcommand(backupSubcommand())
        .subcommand(restoreSubcommand())
        .subcommand(resetSubCommand())
        .subcommand(browseSubcommand())
        .build()

    private val defaultConfigurableList
        get() = listOf(
            modulesConfigurable
        )

    private fun backupSubcommand() = CommandBuilder.begin("backup")
        .parameter(
            Parameters.rootConfigurables()
                .optional()
                .build()
        )
        .handler { command, args ->
            val configurables = args.getOrNull(0) as Set<Configurable>? ?: defaultConfigurableList
            val formattedNames = configurables.joinToString(", ") { configurable ->
                configurable.name.toLowerCamelCase()
            }

            runCatching {
                chat(regular(command.result("backingUp", variable(formattedNames))))
                for (configurable in configurables) {
                    ConfigSystem.storeConfigurable(configurable)
                }

                val fileName = "manual-${LocalDateTime.now().toUnderlinedString()}"
                ConfigSystem.backup(fileName, configurables)
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
                .autocompletedWith { begin, _ ->
                    ConfigSystem.backupFolder.listFiles()
                        ?.map { file -> file.nameWithoutExtension }
                        ?.filter { file -> file.startsWith(begin) }
                        ?: emptyList()
                }
                .required()
                .build()
        )
        .handler { command, args ->
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
            Parameters.rootConfigurables()
                .optional()
                .build()
        )
        .handler { command, args ->
            val configurables = args.getOrNull(0) as Set<Configurable>? ?: defaultConfigurableList
            val formattedNames = configurables.joinToString(", ") { configurable ->
                configurable.name.toLowerCamelCase()
            }

            AutoConfig.withLoading {
                runCatching {
                    chat(regular(command.result("resetting", variable(formattedNames))))

                    for (configurable in configurables) {
                        // TODO: We could straight up use configurable.restore(), however, we
                        //   want to filter out the ModuleHud module

                        for (value in configurable.inner) {
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

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler { command, _ ->
        Util.getOperatingSystem().open(ConfigSystem.backupFolder)
        chat(regular(command.result("browse", variable(ConfigSystem.backupFolder.absolutePath))))
    }.build()

}
