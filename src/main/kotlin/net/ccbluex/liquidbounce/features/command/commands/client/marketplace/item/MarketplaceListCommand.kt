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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.pagination.Pagination
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.TaggedArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.commands.client.config.details
import net.ccbluex.liquidbounce.features.command.commands.client.config.header
import net.ccbluex.liquidbounce.features.command.commands.client.config.plain
import net.ccbluex.liquidbounce.features.command.commands.client.config.quoted
import net.ccbluex.liquidbounce.features.command.commands.client.config.request
import net.ccbluex.liquidbounce.features.command.preset.pageNavigation
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.text.asText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

/**
 * Lists and searches marketplace items
 */
object MarketplaceListCommand {

    fun CmdLiteralScope.list() {
        literal("list") {
            argument(
                "type",
                TaggedArgumentType<MarketplaceItemType>("type") { it.isListable },
            ) { type ->
                optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
                    execSuspend { ctx ->
                        val itemType = ctx.get(type)
                        val response = request { MarketplaceApi.getMarketplaceItems(ctx.get(page), type = itemType) }
                        val pagination = response.pagination
                        header(
                            t("list.header.${itemType.tag.lowercase()}"),
                            t("list.page", pagination.current, pagination.pages)
                        )
                        printItems(response.items, withType = false)
                        printNavigation(pagination) { "marketplace list ${itemType.tag} $it" }
                    }
                }
            }
        }
    }

    fun CmdLiteralScope.search() {
        literal("search") {
            argument("query", ClientStringArgumentType.string()) { query ->
                optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
                    execSuspend { ctx ->
                        val response = request {
                            MarketplaceApi.getMarketplaceItems(ctx.get(page), query = ctx.get(query))
                        }
                        header(t("search.header", ctx.get(query)), t("search.count", response.pagination.items))
                        printItems(response.items, withType = true)
                        printNavigation(response.pagination) { "marketplace search ${quoted(ctx.get(query))} $it" }
                    }
                }
            }
        }
    }

    private fun CmdI18n.printItems(items: List<MarketplaceItem>, withType: Boolean) {
        if (items.isEmpty()) {
            chat(regular(t("list.noItems")), metadata = plain)
            return
        }

        for (item in items) {
            val subscribed = MarketplaceManager.isSubscribed(item.id)
            chat(
                regular("⬥ ").withColor(ChatFormatting.BLUE)
                    .append(addressText(item, subscribed))
                    .apply {
                        if (subscribed) {
                            append(
                                " ●".asText().withStyle(ChatFormatting.GREEN)
                                    .onHover(HoverEvent.ShowText(regular(t("list.subscribed"))))
                            )
                        }
                        if (item.featured) {
                            append(" ★".asText().withStyle(ChatFormatting.GOLD))
                        }
                        if (withType) {
                            append(regular("  ${item.type.tag}"))
                        }
                    },
                metadata = plain
            )
            details(item)?.let { chat(it, metadata = plain) }
        }
    }

    /**
     * `author/name`, clickable to subscribe or, once subscribed, to unsubscribe.
     */
    private fun CmdI18n.addressText(item: MarketplaceItem, subscribed: Boolean): MutableComponent {
        val action = if (subscribed) "unsubscribe" else "subscribe"
        return regular("")
            .apply { item.author?.let { append(regular("$it/")) } }
            .append(variable(item.name))
            .onClick(
                ClickEvent.SuggestCommand(
                    "${CommandManager.GlobalSettings.prefix}marketplace $action ${quoted(item.address)}"
                )
            )
            .onHover(HoverEvent.ShowText(regular(t("list.$action", variable(item.address)))))
    }

    private fun printNavigation(pagination: Pagination, command: (page: Int) -> String) {
        pageNavigation(pagination.current, pagination.pages, command)?.let { chat(it, metadata = plain) }
    }

}
