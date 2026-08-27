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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent

/**
 * Search marketplace items
 */
@Suppress("LongMethod", "CognitiveComplexMethod")
object MarketplaceSearchCommand {

    fun CmdLiteralScope.search() {
        literal("search") {
            argument("query", ClientStringArgumentType.string()) { query ->
                optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
                    execSuspend { ctx ->
                        this@search.search(ctx.get(query), ctx.get(page))
                    }
                }
            }
        }
    }

    private suspend fun CmdI18n.search(query: String, page: Int) {
        chat(regular(t("search.searching")))

        val response = MarketplaceApi.getMarketplaceItems(
            page = page,
            query = query
        )

        if (response.items.isEmpty()) {
            chat(regular(t("search.noResults")))
            return
        }

        chat(
            regular(
                t("search.header",
                    variable(page.toString()),
                    variable(response.pagination.pages.toString())
                )
            )
        )

        for (item in response.items) {
            val isSubscribed = MarketplaceManager.isSubscribed(item.id)
            val action = if (isSubscribed) "unsubscribe" else "subscribe"
            chat(
                regular(
                    t("search.item",
                        variable(item.id.toString()),
                        variable("${item.name}${if (isSubscribed) "*" else ""}"),
                        variable(item.type.toString().lowercase()),
                        variable(if (item.featured) "★" else "")
                    ).onClick(
                        ClickEvent.SuggestCommand(
                            CommandManager.GlobalSettings.prefix + "marketplace $action ${item.id}"
                        )
                    ).onHover(
                        HoverEvent.ShowText(
                            variable(
                                t("search.hover",
                                    variable(action),
                                    item.id
                                )
                            )
                        )
                    )
                )
            )
        }
    }
}
