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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Edits the listing of the tracked config
 */
object ConfigSetCommand {

    private val LIST_SEPARATOR = Regex("[,\\s]+")

    fun CmdLiteralScope.set() {
        literal("set") {
            literal("name") {
                argument("name", ClientStringArgumentType.string()) { name ->
                    execSuspend { ctx -> edit(name = ctx.get(name)) }
                }
            }
            literal("description") {
                argument("description", StringArgumentType.greedyString()) { description ->
                    execSuspend { ctx -> edit(description = ctx.get(description)) }
                }
            }
            literal("tags") {
                // Comma-separated only: tag names can contain spaces.
                optional(
                    "tags",
                    StringArgumentType.greedyString(),
                    default = "",
                    suggests = tagListSuggestions
                ) { tags ->
                    execSuspend { ctx ->
                        val ids = tagIds(ctx.get(tags).split(',').map(String::trim).filter(String::isNotEmpty))
                        edit(details = MarketplaceApi.ItemDetails(tags = ids))
                    }
                }
            }
            literal("servers") {
                optional("servers", StringArgumentType.greedyString(), default = "") { servers ->
                    execSuspend { ctx ->
                        val list = ctx.get(servers).split(LIST_SEPARATOR).filter(String::isNotEmpty)
                        edit(details = MarketplaceApi.ItemDetails(targetServers = list))
                    }
                }
            }
            literal("visibility") {
                argument(
                    "visibility",
                    ClientStringArgumentType.word(),
                    suggests = visibilitySuggestions
                ) { visibility ->
                    execSuspend { ctx ->
                        edit(details = MarketplaceApi.ItemDetails(visibility = parseVisibility(ctx.get(visibility))))
                    }
                }
            }
        }
    }

    /**
     * The API replaces name and description on every update, so the current ones are sent along.
     */
    private suspend fun CmdI18n.edit(
        name: String? = null,
        description: String? = null,
        details: MarketplaceApi.ItemDetails = MarketplaceApi.ItemDetails(),
    ) {
        requireOwnTracked()

        val session = session()
        val updated = request {
            val item = MarketplaceApi.getMarketplaceItem(ConfigTracker.itemId, session)
            MarketplaceApi.updateMarketplaceItem(
                session,
                item.id,
                name ?: item.name,
                MarketplaceItemType.CONFIG,
                description ?: item.description,
                details
            )
        }

        ConfigTracker.renamed(updated)
        chat(regular(t("set.updated", variable(updated.name))))
        updated.shareCode?.let { code ->
            chat(regular(t("info.shareCode", variable(code).copyable())))
        }
    }

}
