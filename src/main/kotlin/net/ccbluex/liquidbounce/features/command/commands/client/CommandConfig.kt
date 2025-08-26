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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.api.services.client.ClientApi
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import org.apache.commons.io.input.CharSequenceReader

/**
 * Config Command
 *
 * Provides various subcommands related to the configuration,
 * such as loading configuration from an external source or an API
 * and listing available configurations.
 */
object CommandConfig : CommandFactory {

    private const val CONFIGS_URL = "https://github.com/CCBlueX/LiquidCloud/tree/main/LiquidBounce/settings/nextgen"

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("config")
            .hub()
            .subcommand(loadSubcommand())
            .subcommand(listSubcommand())
            .subcommand(browseSubcommand())
            .subcommand(reloadSubcommand())
            .build()
    }

    private fun browseSubcommand() = CommandBuilder
        .begin("browse")
        .handler { _, _ ->
            browseUrl(CONFIGS_URL)
        }
        .build()

    private fun reloadSubcommand() = CommandBuilder
        .begin("reload")
        .suspendHandler { command, _ ->
            if (AutoConfig.reloadConfigs()) {
                chat(regular("Reloaded ${configs?.size} settings info from API"))
            } else {
                chat(markAsError("Failed to load settings list from API"))
            }
        }.build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .handler { command, _ ->
            runCatching {
                chat(regular(command.result("loading")))
                val widthOfSpace = mc.textRenderer.getWidth(" ")
                val configs = configs ?: run {
                    chat(markAsError("Failed to load settings list from API"))
                    return@handler
                }
                val width = configs.maxOf { mc.textRenderer.getWidth(it.settingId) }

                // In the case of the chat, we want to show the newest config at the bottom for visibility
                configs.sortedBy { it.date }.forEach {
                    val settingName = it.settingId // there is also .name, but we use it for GUI instead

                    // Append spaces to the setting name to align the date and status
                    // Compensate for the length of the setting name
                    val spaces = " ".repeat(
                        (width - mc.textRenderer.getWidth(settingName))
                            / widthOfSpace
                    )

                    chat(
                        variable(settingName)
                            .onClick(
                                ClickEvent(
                                    ClickEvent.Action.SUGGEST_COMMAND,
                                    ".config load $settingName"
                                )
                            )
                            .onHover(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Text.of("§7Click to load $settingName")
                                )
                            ),
                        regular(spaces),
                        regular(" | "),
                        variable(it.dateFormatted),
                        regular(" | "),
                        Text.literal(it.statusType.displayName)
                            .formatted(it.statusType.formatting)
                            .onHover(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Text.of(it.statusDateFormatted)
                                )
                            )
                        ,
                        regular(" | ${it.serverAddress ?: "Global"}"),
                        metadata = MessageMetadata(prefix = false)
                    )
                }
            }.onFailure {
                chat(markAsError("Failed to load settings list from API"))
            }
        }
        .build()

    private fun loadSubcommand() = CommandBuilder
        .begin("load")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { begin, _ -> this.autocompleteConfigs(begin) }
                .required()
                .build()
        )
        .parameter(
            Parameters.modules()
                .optional()
                .build()
        )
        .suspendHandler { command, args ->
            val name = args[0] as String
            val modules = args.getOrNull(1) as Set<ClientModule>? ?: emptySet()

            runCatching {
                withContext(Dispatchers.IO) {
                    // Read full response to prevent blocking of Reader
                    if (name.startsWith("http")) {
                        // Load the config from the specified URL
                        HttpClient.request(name, HttpMethod.GET).parse<String>()
                    } else {
                        // Get online config from API
                        ClientApi.requestSettingsScript(name).use { it.readText() }
                    }
                }
            }.onSuccess { source ->
                AutoConfig.withLoading {
                    runCatching {
                        AutoConfig.loadAutoConfig(CharSequenceReader(source), modules)
                    }.onFailure {
                        chat(markAsError(command.result("failedToLoad", variable(name))))
                    }.onSuccess {
                        chat(regular(command.result("loaded", variable(name))))
                    }
                }
            }.onFailure { exception ->
                chat(markAsError(command.result("failedToLoad", variable(name))))
                logger.error("Failed to load config $name", exception)
            }
        }
        .build()

    private fun autocompleteConfigs(begin: String): List<String> {
        return configs?.map { it.settingId }?.filter { it.startsWith(begin, true) } ?: emptyList()
    }

}
