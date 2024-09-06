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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.oauth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.oauth.ClientAccountManager
import net.ccbluex.liquidbounce.api.oauth.OAuthClient
import net.ccbluex.liquidbounce.api.oauth.OAuthClient.startAuth
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder.Companion.BOOLEAN_VALIDATOR
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.misc.HideAppearance.destructClient
import net.ccbluex.liquidbounce.features.misc.HideAppearance.wipeClient
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.web.integration.BrowserScreen
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.type.native.NativeTheme
import net.minecraft.util.Util

/**
 * Client Command
 *
 * Provides subcommands for client management.
 */
object CommandClient {

    /**
     * Creates client command with a variety of subcommands.
     */
    fun createCommand() = CommandBuilder.begin("client")
        .hub()
        .subcommand(infoCommand())
        .subcommand(browserCommand())
        .subcommand(integrationCommand())
        .subcommand(languageCommand())
        .subcommand(themeCommand())
        .subcommand(appereanceCommand())
        .subcommand(prefixCommand())
        .subcommand(destructCommand())
        .subcommand(accountCommand())
        .subcommand(cosmeticsCommand())
        .subcommand(resetCommand())
        .build()

    private fun infoCommand() = CommandBuilder
        .begin("info")
        .handler { command, _ ->
            chat(regular(command.result("clientName", variable(LiquidBounce.CLIENT_NAME))),
                prefix = false)
            chat(regular(command.result("clientVersion", variable(LiquidBounce.clientVersion))),
                prefix = false)
            chat(regular(command.result("clientAuthor", variable(LiquidBounce.CLIENT_AUTHOR))),
                prefix = false)
        }.build()

    private fun browserCommand() = CommandBuilder.begin("browser")
        .hub()
        .subcommand(
            CommandBuilder.begin("open")
                .parameter(
                    ParameterBuilder.begin<String>("name")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    chat(regular("Opening browser..."))
                    RenderSystem.recordRenderCall {
                        mc.setScreen(BrowserScreen(args[0] as String))
                    }
                }.build()
        )
        .build()

    private fun integrationCommand() = CommandBuilder.begin("integration")
        .hub()
        .subcommand(CommandBuilder.begin("menu")
            .alias("url")
            .handler { command, args ->
                chat(variable("Client Integration"))
//                val baseUrl = ThemeManager.route().url
//
//                chat(
//                    regular("Base URL: ")
//                        .append(variable(baseUrl).styled {
//                            it.withUnderline(true)
//                                .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, baseUrl))
//                                .withHoverEvent(
//                                    HoverEvent(
//                                        HoverEvent.Action.SHOW_TEXT,
//                                        regular("Click to open the integration URL in your browser.")
//                                    )
//                                )
//                        }),
//                    prefix = false
//                )

//                chat(prefix = false)
//                chat(regular("Integration Menu:"))
//                for (screenType in VirtualScreenType.entries) {
//                    val url = runCatching {
//                        ThemeManager.route(screenType, true)
//                    }.getOrNull()?.url ?: continue
//                    val upperFirstName = screenType.routeName.replaceFirstChar { it.uppercase() }
//
//                    chat(
//                        regular("-> $upperFirstName (")
//                            .append(variable("Browser").styled {
//                                it.withUnderline(true)
//                                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
//                                    .withHoverEvent(
//                                        HoverEvent(
//                                            HoverEvent.Action.SHOW_TEXT,
//                                            regular("Click to open the URL in your browser.")
//                                        )
//                                    )
//                            })
//                            .append(regular(", "))
//                            .append(variable("Clipboard").styled {
//                                it.withUnderline(true)
//                                    .withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, url))
//                                    .withHoverEvent(
//                                        HoverEvent(
//                                            HoverEvent.Action.SHOW_TEXT,
//                                            regular("Click to copy the URL to your clipboard.")
//                                        )
//                                    )
//                            })
//                            .append(regular(")")),
//                        prefix = false
//                    )
//                }
//
//                chat(variable("Hint: You can also access the integration from another device.")
//                    .styled { it.withItalic(true) })
            }.build()
        )
        .subcommand(CommandBuilder.begin("override")
            .parameter(
                ParameterBuilder.begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                chat(regular("Overrides client JCEF browser..."))
//                integrationReference.loadUrl(args[0] as String)
            }.build()
        ).subcommand(CommandBuilder.begin("reset")
            .handler { command, args ->
                chat(regular("Resetting client JCEF browser..."))
                IntegrationHandler.sync()
            }.build()
        )
        .build()

    private fun languageCommand() = CommandBuilder.begin("language")
        .hub()
        .subcommand(CommandBuilder.begin("list")
            .handler { command, args ->
                chat(regular("Available languages:"))
                for (language in LanguageManager.knownLanguages) {
                    chat(regular("-> $language"))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("set")
            .parameter(
                ParameterBuilder.begin<String>("language")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val language = LanguageManager.knownLanguages.find { it.equals(args[0] as String, true) }
                if (language == null) {
                    chat(regular("Language not found."))
                    return@handler
                }

                chat(regular("Setting language to ${language}..."))
                LanguageManager.overrideLanguage = language

                ConfigSystem.storeConfigurable(LanguageManager)
            }.build()
        )
        .subcommand(CommandBuilder.begin("unset")
            .handler { command, args ->
                chat(regular("Unset override language..."))
                LanguageManager.overrideLanguage = ""
                ConfigSystem.storeConfigurable(LanguageManager)
            }.build()
        )
        .build()

    private fun themeCommand() = CommandBuilder.begin("theme")
        .hub()
        .subcommand(CommandBuilder.begin("list")
            .handler { command, args ->
                @Suppress("SpreadOperator")
                chat(
                    regular("Available themes: "),
                    *ThemeManager.availableThemes.flatMapIndexed { index, theme ->
                        listOf(
                            regular(if (index == 0) "" else ", "),
                            variable(theme.name),
                            regular(" (${if (theme is NativeTheme) "Native" else "Web"})")
                        )
                    }.toTypedArray()
                )
            }.build()
        )
        .subcommand(CommandBuilder.begin("set")
            .parameter(
                ParameterBuilder.begin<String>("theme")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .autocompletedWith { s, _ ->
                        ThemeManager.availableThemes
                            .map { theme -> theme.name }
                            .filter { name -> name.startsWith(name, true) }
                    }
                    .build()
            )
            .handler { command, args ->
                val name = args[0] as String

                runCatching {
                    ThemeManager.chooseTheme(name)
                }.onFailure {
                    chat(markAsError("Failed to switch theme: ${it.message}"))
                }.onSuccess {
                    chat(regular("Switched theme to $name."))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("browse").handler { command, _ ->
            Util.getOperatingSystem().open(ThemeManager.themesFolder)
            chat(regular("Location: "), variable(ThemeManager.themesFolder.absolutePath))
        }.build())
        .build()

    private fun appereanceCommand() = CommandBuilder.begin("appearance")
        .hub()
        .subcommand(CommandBuilder.begin("hide")
            .handler { command, args ->
                if (HideAppearance.isHidingNow) {
                    chat(regular(command.result("alreadyHidingAppearance")))
                    return@handler
                }

                chat(regular(command.result("hidingAppearance")))
                HideAppearance.isHidingNow = true
            }.build()
        )
        .subcommand(CommandBuilder.begin("show")
            .handler { command, args ->
                if (!HideAppearance.isHidingNow) {
                    chat(regular(command.result("alreadyShowingAppearance")))
                    return@handler
                }

                chat(regular(command.result("showingAppearance")))
                HideAppearance.isHidingNow = false
            }.build()
        )
        .build()

    private fun destructCommand() = CommandBuilder.begin("destruct")
        .parameter(
            ParameterBuilder.begin<Boolean>("confirm")
                .verifiedBy(BOOLEAN_VALIDATOR)
                .optional()
                .build()
        )
        .parameter(
            ParameterBuilder.begin<Boolean>("wipe")
                .verifiedBy(BOOLEAN_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, args ->
            val confirm = args.getOrNull(0) as Boolean? ?: false
            if (!confirm) {
                chat(regular("Do you really want to destruct the client? " +
                    "If so, type the command again with 'yes' at the end."))
                chat(markAsError("If you also want to wipe the client, add an additional 'yes' at the end."))
                chat(regular("For full destruct: .client destruct yes yes"))
                chat(regular("For temporary destruct: .client destruct yes"))
                return@handler
            }

            val wipe = args.getOrNull(1) as Boolean? ?: false

            chat(regular("LiquidBounce is being destructed from your client..."))
            if (!wipe) {
                chat(regular("WARNING: You have not wiped the client (missing wipe parameter) - therefore " +
                    "some files may still be present!"))
            }

            destructClient()
            chat(regular("LiquidBounce has been destructed from your client. " +
                "You can clear your chat using F3+D. If wipe was enabled, the chat will be cleared automatically."))

            if (wipe) {
                chat(regular("Wiping client..."))
                // Runs on a separate thread to prevent blocking the main thread and
                // repeating the process when required
                wipeClient()
            }
        }.build()

    private fun prefixCommand() = CommandBuilder.begin("prefix")
        .parameter(
            ParameterBuilder
                .begin<String>("prefix")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .handler { command, args ->
            val prefix = args[0] as String
            CommandManager.Options.prefix = prefix
            chat(regular(command.result("prefixChanged", variable(prefix))))
        }
        .build()

    private fun accountCommand() = CommandBuilder.begin("account")
        .hub()
        .subcommand(CommandBuilder.begin("login")
            .handler { command, args ->
                if (ClientAccountManager.clientAccount != EMPTY_ACCOUNT) {
                    chat(regular("You are already logged in."))
                    return@handler
                }

                chat(regular("Starting OAuth authorization process..."))
                OAuthClient.runWithScope {
                    val account = startAuth { Util.getOperatingSystem().open(it) }
                    ClientAccountManager.clientAccount = account
                    ConfigSystem.storeConfigurable(ClientAccountManager)
                    chat(regular("Successfully authorized client."))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("logout")
            .handler { command, args ->
                if (ClientAccountManager.clientAccount == EMPTY_ACCOUNT) {
                    chat(regular("You are not logged in."))
                    return@handler
                }

                chat(regular("Logging out..."))
                OAuthClient.runWithScope {
                    ClientAccountManager.clientAccount = EMPTY_ACCOUNT
                    ConfigSystem.storeConfigurable(ClientAccountManager)
                    chat(regular("Successfully logged out."))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("info")
            .handler { command, args ->
                if (ClientAccountManager.clientAccount == EMPTY_ACCOUNT) {
                    chat(regular("You are not logged in."))
                    return@handler
                }

                chat(regular("Getting user information..."))
                OAuthClient.runWithScope {
                    runCatching {
                        val account = ClientAccountManager.clientAccount
                        account.updateInfo()
                        account
                    }.onSuccess { account ->
                        account.userInformation?.let { info ->
                            chat(regular("User ID: "), variable(info.userId))
                            chat(regular("Donation Perks: "), variable(if (info.premium) "Yes" else "No"))
                        }
                    }.onFailure {
                        chat(markAsError("Failed to get user information: ${it.message}"))
                    }

                }
            }.build()
        )
        .build()

    private fun resetCommand() = CommandBuilder
        .begin("reset")
        .handler { command, _ ->
            AutoConfig.loadingNow = true
            ModuleManager
                // TODO: Remove when HUD no longer contains the Element Configuration
                .filter { module -> module !is ModuleHud  }
                .forEach { it.restore() }
            AutoConfig.loadingNow = false
            chat(regular(command.result("successfullyReset")))
        }
        .build()

    private fun cosmeticsCommand() = CommandBuilder
        .begin("cosmetics")
        .hub()
        .subcommand(
            CommandBuilder.begin("refresh")
                .handler { command, _ ->
                    chat(regular("Refreshing cosmetics..."))
                    CosmeticService.carriersCosmetics.clear()
                    ClientAccountManager.clientAccount.cosmetics = null

                    CosmeticService.refreshCarriers(true) {
                        chat(regular("Cosmetic System has been refreshed."))
                    }
                }
                .build()
        )
        .subcommand(
            CommandBuilder.begin("manage")
                .handler { _, _ ->
                    browseUrl("https://user.liquidbounce.net/cosmetics")
                }
                .build()
        )
        .build()

}
