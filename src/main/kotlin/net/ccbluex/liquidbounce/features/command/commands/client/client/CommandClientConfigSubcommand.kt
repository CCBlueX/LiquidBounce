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
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.MultiSelectArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.toLowerCamelCase
import net.minecraft.util.Util
import java.time.LocalDateTime

/**
 * Configurable Management Command
 *
 * Allows you to back up, restore, reset, and browse configurations.
 */
object CommandClientConfigSubcommand {
    private val defaultConfigs
        get() = listOf(
            net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfig
        )

    private val configArg = MultiSelectArgumentType(
        "Config", ConfigSystem.configs, predicate = { true }, nameOf = Config::name)

    fun CmdLiteralScope.config() {
        literal("config") {
            literal("backup") {
                optional(
                    "configs",
                    configArg,
                    default = null,
                ) { configs ->
                    exec { ctx ->
                        backup(ctx.get(configs) ?: emptySet())
                    }
                }
            }
            literal("restore") {
                argument(
                    "name",
                    ClientStringArgumentType.word(),
                    suggestions(Util.nonCriticalIoPool()) {
                        ConfigSystem.backupFolder.listFiles()
                            ?.map { file -> file.nameWithoutExtension }
                    },
                ) { name ->
                    exec { ctx ->
                        restore(ctx.get(name))
                    }
                }
            }
            literal("reset") {
                optional(
                    "configs",
                    configArg,
                    default = null,
                ) { configs ->
                    exec { ctx ->
                        reset(ctx.get(configs) ?: emptySet())
                    }
                }
            }
            literal("browse") {
                exec {
                    browse()
                }
            }
        }
    }

    private fun CmdI18n.backup(configs: Set<Config>): Int {
        val effectiveConfigs = configs.ifEmpty { defaultConfigs }
        val formattedNames = effectiveConfigs.joinToString(", ") { config ->
            config.name.toLowerCamelCase()
        }

        runCatching {
            chat(regular(t("config.backup.backingUp", variable(formattedNames))))
            for (config in effectiveConfigs) {
                ConfigSystem.store(config)
            }

            val fileName = "manual-${LocalDateTime.now().toUnderlinedString()}"
            ConfigSystem.backup(fileName, effectiveConfigs)
            fileName
        }.onFailure { exception ->
            chat(markAsError(t("config.backup.failedToBackup", exception.message ?: "Unknown error")))
        }.onSuccess { fileName ->
            chat(regular(t("config.backup.backedUp", variable(fileName))))
            chat(
                regular(
                    t(
                        "config.backup.restoreHelp",
                        variable(
                            "${CommandManager.GlobalSettings.prefix}client config restore $fileName"
                        ),
                    )
                )
            )
        }
        return 1
    }

    private fun CmdI18n.restore(fileName: String): Int {
        AutoConfig.withLoading {
            runCatching {
                chat(regular(t("config.restore.restoring", variable(fileName))))

                ConfigSystem.restore(fileName)
            }.onFailure { exception ->
                chat(markAsError(t("config.restore.failedToRestore", exception.message ?: "Unknown error")))
            }.onSuccess {
                chat(regular(t("config.restore.restored", variable(fileName))))
            }
        }
        return 1
    }

    @Suppress("CognitiveComplexMethod")
    private fun CmdI18n.reset(configs: Set<Config>): Int {
        val effectiveConfigs = configs.ifEmpty { defaultConfigs }
        val formattedNames = effectiveConfigs.joinToString(", ") { config ->
            config.name.toLowerCamelCase()
        }

        AutoConfig.withLoading {
            runCatching {
                chat(regular(t("config.reset.resetting", variable(formattedNames))))

                for (config in effectiveConfigs) {
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
                chat(regular(t("config.reset.reset")))
            }.onFailure { exception ->
                chat(markAsError(t("config.reset.failedToReset", exception.message ?: "Unknown error")))
            }
        }
        return 1
    }

    private fun CmdI18n.browse(): Int {
        Util.getPlatform().openFile(ConfigSystem.backupFolder)
        chat(regular(t("config.browse.browse", variable(ConfigSystem.backupFolder.absolutePath))))
        return 1
    }

}
