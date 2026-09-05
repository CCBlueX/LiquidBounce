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
import net.ccbluex.liquidbounce.features.addon.AddonManager
import net.ccbluex.liquidbounce.features.addon.AddonState
import net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdChainScope
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Inspects installed add-ons.
 *
 * There is deliberately no `reload`: add-ons are Fabric mods, so installing or removing one only
 * takes effect after a restart.
 */
object CommandAddon : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("addon") {
            literal("list") {
                exec {
                    listAddons()
                }
            }
            literal("info") {
                addonNameArgument { name ->
                    exec { ctx ->
                        addonInfo(ctx.get(name))
                    }
                }
            }
            literal("enable") {
                addonNameArgument { name ->
                    exec { ctx ->
                        setEnabled(ctx.get(name), enabled = true)
                    }
                }
            }
            literal("disable") {
                addonNameArgument { name ->
                    exec { ctx ->
                        setEnabled(ctx.get(name), enabled = false)
                    }
                }
            }
        }
    }

    private fun CmdLiteralScope.addonNameArgument(block: CmdChainScope.ArgContinuation<String>) =
        argument(
            "name",
            ClientStringArgumentType.word(),
            suggestions(strings = { AddonManager.addons.map { it.id } }),
            block,
        )

    private fun CmdI18n.listAddons(): Int {
        val addons = AddonManager.addons

        if (addons.isEmpty()) {
            chat(regular(t("list.noAddons")))
            return 1
        }

        chat(regular(t("list.addons", variable(addons.joinToString(", ") { "${it.id} (${it.version})" }))))
        reportPendingRestart()
        return 1
    }

    private fun CmdI18n.addonInfo(id: String): Int {
        val addon = AddonManager[id] ?: run {
            chat(regular(t("info.notFound", variable(id))))
            return 1
        }

        chat(regular(t("info.name", variable(addon.displayName), variable(addon.version))))
        chat(regular(t("info.state", variable(addon.state.name))))

        if (addon.authors.isNotEmpty()) {
            chat(regular(t("info.authors", variable(addon.authors.joinToString(", ")))))
        }
        if (addon.description.isNotBlank()) {
            chat(regular(t("info.about", variable(addon.description))))
        }
        addon.metadata.sources?.let { chat(regular(t("info.sources", variable(it)))) }

        chat(regular(t("info.features", variable(featureSummary(addon)))))
        return 1
    }

    private fun featureSummary(addon: LiquidBounceAddon) = buildString {
        append(addon.registeredModules.size).append(" modules, ")
        append(addon.registeredCommands.size + addon.registeredNodes.size).append(" commands, ")
        append(addon.registeredCategories.size).append(" categories")
    }

    private fun CmdI18n.setEnabled(id: String, enabled: Boolean): Int {
        val addon = AddonManager[id] ?: run {
            chat(regular(t("info.notFound", variable(id))))
            return 1
        }

        if (enabled) {
            // An add-on's classes stay loaded, but its onInitialize cannot safely be replayed:
            // settings were restored at startup and would be lost. A restart is the honest answer.
            chat(regular(t("enable.restartRequired", variable(addon.id))))
            return 1
        }

        if (addon.state == AddonState.DISABLED) {
            chat(regular(t("disable.alreadyDisabled", variable(addon.id))))
            return 1
        }

        AddonManager.disable(addon)
        chat(regular(t("disable.disabled", variable(addon.id))))
        return 1
    }

    private fun CmdI18n.reportPendingRestart() {
        if (!AddonManager.pendingRestart) {
            return
        }

        chat(regular(t("list.restartRequired", variable(AddonManager.restartReasons.joinToString(", ")))))
    }

}
