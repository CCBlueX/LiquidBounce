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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.api.services.client.ClientApi
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig.configs
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfigMetadata
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.modules
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.plus
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.textOf
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.AsyncLoadingText
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.apache.commons.io.input.CharSequenceReader

/**
 * Config Command
 *
 * Provides various subcommands related to the configuration,
 * such as loading configuration from an external source or an API
 * and listing available configurations.
 */
object CommandConfig : Command.Factory {

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

    private fun hoverText(settingName: String) =
        textOf(
            "Click to load ".asPlainText(ChatFormatting.GRAY),
            settingName.asPlainText(Style.EMPTY + ChatFormatting.AQUA + ChatFormatting.BOLD),
            PlainText.NEW_LINE,
            AsyncLoadingText(
                ioScope.async {
                    ClientApi.requestSettingsScript(settingName).use { r ->
                        publicGson.fromJson(r, AutoConfigMetadata::class.java)
                    }.asText()
                }
            )
        )

    private fun browseSubcommand() = CommandBuilder
        .begin("browse")
        .handler {
            browseUrl(CONFIGS_URL)
        }
        .build()

    private fun reloadSubcommand() = CommandBuilder
        .begin("reload")
        .suspendHandler {
            if (AutoConfig.reloadConfigs()) {
                chat(regular("Reloaded ${configs?.size} settings info from API"))
            } else {
                chat(markAsError("Failed to load settings list from API"))
            }
        }.build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .handler {
            runCatching {
                chat(regular(command.result("loading")))
                val widthOfSpace = mc.font.width(" ")
                val configs = configs ?: run {
                    chat(markAsError("Failed to load settings list from API"))
                    return@handler
                }
                val width = configs.maxOf { mc.font.width(it.settingId) }

                // In the case of the chat, we want to show the newest config at the bottom for visibility
                configs.sortedBy { it.date }.forEach {
                    val settingName = it.settingId // there is also .name, but we use it for GUI instead

                    // Append spaces to the setting name to align the date and status
                    // Compensate for the length of the setting name
                    val spaces = " ".repeat(
                        (width - mc.font.width(settingName))
                            / widthOfSpace
                    )

                    chat(
                        variable(settingName)
                            .onClick(
                                ClickEvent.SuggestCommand(
                                    CommandManager.GlobalSettings.prefix + "config load $settingName"
                                )
                            )
                            .onHover(HoverEvent.ShowText(hoverText(settingName))),
                        regular(spaces),
                        regular(" | "),
                        variable(it.dateFormatted),
                        regular(" | "),
                        Component.literal(it.statusType.displayName)
                            .withStyle(it.statusType.formatting)
                            .onHover(
                                HoverEvent.ShowText(it.statusDateFormatted.asPlainText())
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
                .autocompletedFrom { configs?.map { it.settingId } }
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder.modules()
                .optional()
                .build()
        )
        .suspendHandler {
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

}
