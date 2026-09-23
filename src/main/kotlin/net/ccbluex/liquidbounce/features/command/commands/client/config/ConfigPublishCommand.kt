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

import com.mojang.brigadier.arguments.StringArgumentType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain

/**
 * Publishes the current settings to the marketplace
 */
object ConfigPublishCommand {

    fun CmdLiteralScope.publish() {
        literal("publish") {
            publishArguments { name, visibility, description ->
                val server = mc.currentServer?.ip?.dropPort()?.rootDomain()
                val item = request {
                    ConfigTracker.create(
                        session(),
                        name,
                        description,
                        MarketplaceApi.ItemDetails(
                            visibility = parseVisibility(visibility),
                            targetServers = listOfNotNull(server)
                        )
                    )
                }
                published(item)
            }
        }
    }

    fun CmdLiteralScope.fork() {
        literal("fork") {
            publishArguments { name, visibility, description ->
                requireTracked()
                if (ownUserId() == ConfigTracker.itemUid) {
                    throw CommandException(t("fork.ownConfig", variable(ConfigTracker.itemName)))
                }
                if (ConfigTracker.state != ConfigTracker.State.EDITING) {
                    throw CommandException(t("fork.notEditing", variable(ConfigTracker.itemName)))
                }

                val item = request {
                    ConfigTracker.fork(session(), name, description, parseVisibility(visibility))
                }
                published(item)
            }
        }
    }

    fun CmdLiteralScope.overlay() {
        literal("overlay") {
            publishArguments { name, visibility, description ->
                requireTracked()
                if (ConfigTracker.state != ConfigTracker.State.EDITING) {
                    throw CommandException(t("overlay.notEditing", variable(ConfigTracker.itemName)))
                }

                val base = ConfigTracker.itemName
                val item = request {
                    ConfigTracker.overlay(session(), name, description, parseVisibility(visibility))
                }
                published(item)
                chat(regular(t("overlay.basedOn", variable(base))))
            }
        }
    }

    fun CmdLiteralScope.update() {
        literal("update") {
            optional("changelog", StringArgumentType.greedyString(), default = null) { changelog ->
                execSuspend { ctx ->
                    requireOwnTracked()
                    if (ConfigTracker.state != ConfigTracker.State.EDITING) {
                        throw CommandException(t("update.notEditing", variable(ConfigTracker.itemName)))
                    }

                    request { ConfigTracker.update(session(), ctx.get(changelog)) }
                    chat(regular(t("update.updated", variable(ConfigTracker.itemName))))
                }
            }
        }
    }

    fun CmdLiteralScope.delete() {
        literal("delete") {
            execSuspend {
                requireOwnTracked()
                val name = ConfigTracker.itemName
                request { ConfigTracker.delete(session()) }
                MarketplaceConfigs.refresh()
                chat(regular(t("delete.deleted", variable(name))))
            }
        }
    }

    private fun CmdLiteralScope.publishArguments(
        handler: suspend CmdI18n.(name: String, visibility: String?, description: String) -> Unit
    ) {
        argument("name", ClientStringArgumentType.string()) { name ->
            optional("visibility", ClientStringArgumentType.word(), default = null, suggests = visibilitySuggestions) {
                    visibility ->
                optional("description", StringArgumentType.greedyString(), default = "") { description ->
                    execSuspend { ctx ->
                        handler(ctx.get(name), ctx.get(visibility), ctx.get(description))
                    }
                }
            }
        }
    }

    private fun CmdI18n.published(item: MarketplaceItem) {
        chat(regular(t("publish.published", variable(item.name))))
        item.shareCode?.let { code ->
            chat(regular(t("info.shareCode", variable(code).copyable())))
        }
    }

}
