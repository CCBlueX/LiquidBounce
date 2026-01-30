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
package net.ccbluex.liquidbounce.features.command.commands.client

import kotlinx.coroutines.async
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.models.client.AutoSettings
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfigMetadata
import net.ccbluex.liquidbounce.config.autoconfig.IncludeConfiguration
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.boolean
import net.ccbluex.liquidbounce.features.command.builder.modules
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.plus
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.textOf
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.io.ILLEGAL_FILE_NAME_CHARS_WINDOWS
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import net.ccbluex.liquidbounce.utils.text.AsyncLoadingText
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.util.Util
import java.io.File
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

    private fun hoverText(file: File, settingName: String) =
        textOf(
            "Click to load ".asPlainText(ChatFormatting.GRAY),
            settingName.asPlainText(Style.EMPTY + ChatFormatting.AQUA + ChatFormatting.BOLD),
            PlainText.NEW_LINE,
            AsyncLoadingText(
                ioScope.async {
                    file.bufferedReader().use { r ->
                        publicGson.fromJson(r, AutoConfigMetadata::class.java)
                    }.asText()
                }
            )
        )

    private fun saveSubcommand() = CommandBuilder
        .begin("save")
        .alias("create")
        .parameter(
            ParameterBuilder.begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedFrom {
                    ConfigSystem.userConfigsFolder.listFiles()?.map { it.nameWithoutExtension }
                }
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder.boolean("overwrite")
                .optional()
                .build()
        )
        .parameter(
            ParameterBuilder.begin<String>("include")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedFrom { listOf("binds", "hidden") }
                .vararg()
                .optional()
                .build()
        )
        .handler {
            val name = args[0] as String

            if (name.isBlank() || name.contains('/') ||
                (Util.getPlatform() == Util.OS.WINDOWS && name.any { ILLEGAL_FILE_NAME_CHARS_WINDOWS.get(it.code) })
            ) {
                throw CommandException(command.result("invalidFileName", variable(name)))
            }

            val overwrite = args.getOrNull(1) as Boolean? ?: false
            @Suppress("UNCHECKED_CAST")
            val include = args.getOrNull(2) as Array<*>? ?: emptyArray<String>()

            val includeConfiguration = IncludeConfiguration(
                includeBinds = include.contains("binds"),
                includeHidden = include.contains("hidden"),
            )

            val file = ConfigSystem.userConfigsFolder.resolve("$name.json")
            try {
                if (file.exists()) {
                    if (overwrite) {
                        file.delete()
                    } else {
                        chat(markAsError(command.result("alreadyExists", variable(name))))
                        return@handler
                    }
                }

                file.createNewFile()
                serializeAutoConfig(file.bufferedWriter(), includeConfiguration)
                chat(regular(command.result("created", variable(name))))
            } catch (e: Exception) {
                chat(regular(command.result("failedToCreate", variable(name))))
                logger.error("Failed to create local config '$name'", e)
            }
        }
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler {
        Util.getPlatform().openFile(ConfigSystem.userConfigsFolder)
        chat(regular(command.result("browse", clickablePath(ConfigSystem.userConfigsFolder))))
    }.build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .pagedQuery(
            pageSize = 8,
            header = {
                highlight("Local Configs:")
            },
            items = {
                ConfigSystem.userConfigsFolder.listFiles { _, name ->
                    name.endsWith(".json", ignoreCase = true)
                }.unmodifiable()
            },
            eachRow = { _, file ->
                val settingName = file.name.removeSuffix(".json")

                val lastModified = Instant.ofEpochMilli(file.lastModified())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(AutoSettings.FORMATTER)

                textOf(
                    "\u2B25 ".asPlainText(ChatFormatting.BLUE),
                    variable(file.name)
                        .onClick(
                            ClickEvent.SuggestCommand(
                                CommandManager.GlobalSettings.prefix + "localconfig load $settingName"
                            )
                        )
                        .onHover(HoverEvent.ShowText(hoverText(file, settingName))),
                    regular(" ($lastModified)"),
                )
            }
        )

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
