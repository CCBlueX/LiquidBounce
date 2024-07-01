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
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder.Companion.BOOLEAN_VALIDATOR
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.misc.HideAppearance.destructClient
import net.ccbluex.liquidbounce.features.misc.HideAppearance.wipeClient
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.web.integration.BrowserScreen
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.clientJcef
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.Theme
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.web.theme.component.components
import net.ccbluex.liquidbounce.web.theme.component.customComponents
import net.ccbluex.liquidbounce.web.theme.component.types.FrameComponent
import net.ccbluex.liquidbounce.web.theme.component.types.HtmlComponent
import net.ccbluex.liquidbounce.web.theme.component.types.ImageComponent
import net.ccbluex.liquidbounce.web.theme.component.types.TextComponent
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
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
        .subcommand(componentCommand())
        .subcommand(appereanceCommand())
        .subcommand(prefixCommand())
        .subcommand(destructCommand())
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
                val baseUrl = ThemeManager.route().url

                chat(
                    regular("Base URL: ")
                        .append(variable(baseUrl).styled {
                            it.withUnderline(true)
                                .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, baseUrl))
                                .withHoverEvent(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        regular("Click to open the integration URL in your browser.")
                                    )
                                )
                        }),
                    prefix = false
                )

                chat(prefix = false)
                chat(regular("Integration Menu:"))
                for (screenType in VirtualScreenType.entries) {
                    val url = runCatching {
                        ThemeManager.route(screenType, true)
                    }.getOrNull()?.url ?: continue
                    val upperFirstName = screenType.routeName.replaceFirstChar { it.uppercase() }

                    chat(
                        regular("-> $upperFirstName (")
                            .append(variable("Browser").styled {
                                it.withUnderline(true)
                                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                    .withHoverEvent(
                                        HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            regular("Click to open the URL in your browser.")
                                        )
                                    )
                            })
                            .append(regular(", "))
                            .append(variable("Clipboard").styled {
                                it.withUnderline(true)
                                    .withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, url))
                                    .withHoverEvent(
                                        HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            regular("Click to copy the URL to your clipboard.")
                                        )
                                    )
                            })
                            .append(regular(")")),
                        prefix = false
                    )
                }

                chat(variable("Hint: You can also access the integration from another device.")
                    .styled { it.withItalic(true) })
            }.build()
        )
        .subcommand(CommandBuilder.begin("override")
            .parameter(
                ParameterBuilder.begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                chat(regular("Overrides client JCEF browser..."))
                clientJcef.loadUrl(args[0] as String)
            }.build()
        ).subcommand(CommandBuilder.begin("reset")
            .handler { command, args ->
                chat(regular("Resetting client JCEF browser..."))
                IntegrationHandler.updateIntegrationBrowser()
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
                chat(regular("Available themes:"))
                for (theme in ThemeManager.themesFolder.listFiles()!!) {
                    chat(regular("-> ${theme.name}"))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("set")
            .parameter(
                ParameterBuilder.begin<String>("theme")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val name = args[0] as String

                if (name.equals("default", true)) {
                    ThemeManager.activeTheme = ThemeManager.defaultTheme
                    chat(regular("Switching theme to default..."))
                    return@handler
                }

                val theme = ThemeManager.themesFolder.listFiles()?.find {
                    it.name.equals(name, true)
                }

                if (theme == null) {
                    chat(regular("Theme not found."))
                    return@handler
                }

                chat(regular("Switching theme to ${theme.name}..."))
                ThemeManager.activeTheme = Theme(theme.name)
            }.build()
        )
        .subcommand(CommandBuilder.begin("browse").handler { command, _ ->
            Util.getOperatingSystem().open(ThemeManager.themesFolder)
            chat(regular("Location: "), variable(ThemeManager.themesFolder.absolutePath))
        }.build())
        .build()

    private fun componentCommand() = CommandBuilder.begin("component")
        .hub()
        .subcommand(CommandBuilder.begin("list")
            .handler { command, args ->
                chat(regular("In-built:"))
                for (component in components) {
                    chat(regular("-> ${component.name}"))
                }

                chat(regular("Custom:"))
                for ((index, component) in customComponents.withIndex()) {
                    chat(regular("-> ${component.name} (#$index}"))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("add")
            .hub()
            .subcommand(CommandBuilder.begin("text")
                .parameter(
                    ParameterBuilder.begin<String>("text")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    customComponents += TextComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added text component.")
                }.build()
            )
            .subcommand(CommandBuilder.begin("frame")
                .parameter(
                    ParameterBuilder.begin<String>("url")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    customComponents += FrameComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added frame component.")
                }.build()
            )
            .subcommand(CommandBuilder.begin("image")
                .parameter(
                    ParameterBuilder.begin<String>("url")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    customComponents += ImageComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added image component.")
                }.build()
            )
            .subcommand(CommandBuilder.begin("html")
                .parameter(
                    ParameterBuilder.begin<String>("code")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    customComponents += HtmlComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added html component.")
                }.build()
            ).build()
        )
        .subcommand(CommandBuilder.begin("remove")
            .parameter(
                ParameterBuilder.begin<Int>("id")
                    .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val index = args[0] as Int
                val component = customComponents.getOrNull(index)

                if (component == null) {
                    chat(regular("Component ID is out of range."))
                    return@handler
                }

                customComponents -= component
                ComponentOverlay.fireComponentsUpdate()
                chat("Successfully removed component.")
            }.build()
        )
        .subcommand(CommandBuilder.begin("clear")
            .handler { command, args ->
                customComponents.clear()
                ComponentOverlay.fireComponentsUpdate()

                chat("Successfully cleared components.")
            }.build()
        )
        .subcommand(CommandBuilder.begin("update")
            .handler { command, args ->
                ComponentOverlay.fireComponentsUpdate()

                chat("Successfully updated components.")
            }.build()
        )
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
            val confirm = args[0] as? Boolean ?: false
            if (!confirm) {
                chat(regular("Do you really want to destruct the client? " +
                    "If so, type the command again with 'yes' at the end."))
                chat(markAsError("If you also want to wipe the client, add an additional 'yes' at the end."))
                chat(regular("For full destruct: .client destruct yes yes"))
                chat(regular("For temporary destruct: .client destruct yes"))
                return@handler
            }

            val wipe = args[1] as? Boolean ?: false

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

}
