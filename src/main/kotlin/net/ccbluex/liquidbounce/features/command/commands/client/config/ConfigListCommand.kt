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

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import net.ccbluex.liquidbounce.features.command.CommandExecutor
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.onClickRun
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.minecraft.ChatFormatting

/**
 * Lists and searches marketplace configs
 */
object ConfigListCommand {

    fun CmdLiteralScope.list() {
        literal("list") {
            optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
                execSuspend { ctx ->
                    val response = request { MarketplaceConfigs.list(page = ctx.get(page)) }
                    if (ctx.get(page) == 1) {
                        MarketplaceConfigs.refresh()
                    }
                    header(t("list.header"), t("list.page", response.pagination.current, response.pagination.pages))
                    printItems(response.items)
                    pageNavigation(response)
                }
            }
        }
    }

    fun CmdLiteralScope.search() {
        literal("search") {
            argument("query", ClientStringArgumentType.string()) { query ->
                optional("tag", ClientStringArgumentType.string(), default = null, suggests = tagSuggestions) { tag ->
                    execSuspend { ctx ->
                        val tags = tagIds(listOfNotNull(ctx.get(tag)))
                        val response = request { MarketplaceConfigs.list(query = ctx.get(query), tags = tags) }
                        header(t("search.header", ctx.get(query)), t("search.count", response.pagination.items))
                        printItems(response.items)
                    }
                }
            }
        }
    }

    private fun CmdI18n.printItems(items: List<MarketplaceItem>) {
        if (items.isEmpty()) {
            chat(regular(t("list.noConfigs")), metadata = plain)
            return
        }

        for (item in items) {
            chat(
                regular("⬥ ").withColor(ChatFormatting.BLUE)
                    .append(addressText(item))
                    .apply { trackedMarker(item)?.let(::append) }
                    .append(regular("  "))
                    .append(votes(item)),
                metadata = plain
            )
            details(item)?.let { chat(it, metadata = plain) }
        }
    }

    private fun CmdI18n.pageNavigation(response: PaginatedResponse<MarketplaceItem>) {
        val (current, pages) = response.pagination.current to response.pagination.pages
        if (pages <= 1) {
            return
        }

        fun arrow(symbol: String, page: Int) = if (page in 1..pages) {
            variable(symbol).onClickRun {
                runCatching { CommandManager.execute("config list $page") }
                    .onFailure(CommandExecutor::handleExceptions)
            }
        } else {
            regular(symbol).withColor(ChatFormatting.DARK_GRAY)
        }

        chat(
            arrow("«", current - 1)
                .append(regular("  ${t("list.page", current, pages).string}  "))
                .append(arrow("»", current + 1)),
            metadata = plain
        )
    }

}
