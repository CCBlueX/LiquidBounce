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

import com.mojang.brigadier.CommandDispatcher
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
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.MultiSelectArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.AsyncLoadingText
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
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
object CommandConfig : CommandRegistrar {
    private const val CONFIGS_URL = "https://github.com/CCBlueX/LiquidCloud/tree/main/LiquidBounce/settings/nextgen"

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("config") {
            literal("load") {
                argument(
                    "name",
                    ClientStringArgumentType.word(),
                    suggestions { configs?.map { it.settingId } },
                ) { name ->
                    optional(
                        "modules",
                        MultiSelectArgumentType(
                            "Module",
                            ModuleManager,
                            predicate = { true },
                            nameOf = ClientModule::name
                        ),
                        default = null,
                    ) { modules ->
                        execSuspend { ctx ->
                            loadConfig(ctx.get(name), ctx.get(modules) ?: emptySet())
                        }
                    }
                }
            }
            literal("list") {
                exec {
                    listConfigs()
                }
            }
            literal("browse") {
                exec {
                    browseConfigs()
                }
            }
            literal("reload") {
                execSuspend {
                    reloadConfigs()
                }
            }
        }
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

    private fun CmdI18n.browseConfigs(): Int {
        browseUrl(CONFIGS_URL)
        return 1
    }

    private suspend fun reloadConfigs() {
        if (AutoConfig.reloadConfigs()) {
            chat(regular("Reloaded ${configs?.size} settings info from API"))
        } else {
            chat(markAsError("Failed to load settings list from API"))
        }
    }

    private fun CmdI18n.listConfigs(): Int {
        runCatching {
            chat(regular(t("list.loading")))
            val widthOfSpace = mc.font.width(" ")
            val configs = configs ?: run {
                chat(markAsError("Failed to load settings list from API"))
                return 1
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
                    it.statusType.displayName.asPlainText(
                        Style.EMPTY +
                            it.statusType.formatting +
                            HoverEvent.ShowText(it.statusDateFormatted.asPlainText())
                    ),
                    regular(" | ${it.serverAddress ?: "Global"}"),
                    metadata = MessageMetadata(prefix = false)
                )
            }
        }.onFailure {
            chat(markAsError("Failed to load settings list from API"))
        }
        return 1
    }

    private suspend fun CmdI18n.loadConfig(name: String, modules: Set<ClientModule>) {
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
                    chat(markAsError(t("load.failedToLoad", variable(name))))
                }.onSuccess {
                    chat(regular(t("load.loaded", variable(name))))
                }
            }
        }.onFailure { exception ->
            chat(markAsError(t("load.failedToLoad", variable(name))))
            logger.error("Failed to load config $name", exception)
        }
    }

}
