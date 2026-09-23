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
package net.ccbluex.liquidbounce.features.command.commands.client.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.MultiSelectArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Loads a marketplace config and moves between it, local edits and the backup
 */
object ConfigLoadCommand {

    fun CmdLiteralScope.load() {
        literal("load") {
            argument("config", ClientStringArgumentType.string(), suggests = configSuggestions) { config ->
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
                        load(ctx.get(config), ctx.get(modules) ?: emptySet())
                    }
                }
            }
        }
    }

    fun CmdLiteralScope.revert() {
        literal("revert") {
            execSuspend {
                requireTracked()
                request { ConfigTracker.revert() }
                chat(regular(t("revert.reverted", variable(ConfigTracker.itemName))))
            }
        }
    }

    fun CmdLiteralScope.restore() {
        literal("restore") {
            execSuspend {
                if (!ConfigTracker.hasBackup) {
                    throw CommandException(t("restore.noBackup"))
                }
                request { ConfigTracker.restoreBackup() }
                chat(regular(t("restore.restored")))
            }
        }
    }

    fun CmdLiteralScope.detach() {
        literal("detach") {
            execSuspend {
                requireTracked()
                val name = ConfigTracker.itemName
                ConfigTracker.detach()
                chat(regular(t("detach.detached", variable(name))))
            }
        }
    }

    private suspend fun CmdI18n.load(input: String, modules: Set<ClientModule>) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            request {
                val source = withContext(Dispatchers.IO) {
                    HttpClient.request(input, HttpMethod.GET).parse<String>()
                }
                ConfigTracker.loadExternal(source, modules)
            }
            chat(regular(t("load.loaded", variable(input))))
            return
        }

        val item = resolveConfig(input)
        val revisionId = item.liveRevisionId ?: throw CommandException(t("load.noRevision", variable(item.name)))
        val result = request { ConfigTracker.load(item, revisionId, modules) }
        chat(regular(t("load.loaded", variable(item.name))))
        reportInstalled(result)
    }

}
