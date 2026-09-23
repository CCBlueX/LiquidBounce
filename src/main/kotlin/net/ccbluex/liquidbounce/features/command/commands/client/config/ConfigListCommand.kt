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
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent

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
                    printPage(response)
                }
            }
        }
    }

    fun CmdLiteralScope.search() {
        literal("search") {
            argument("query", ClientStringArgumentType.string()) { query ->
                optional("tag", ClientStringArgumentType.word(), default = null) { tag ->
                    execSuspend { ctx ->
                        val tags = tagIds(listOfNotNull(ctx.get(tag)))
                        printPage(request { MarketplaceConfigs.list(query = ctx.get(query), tags = tags) })
                    }
                }
            }
        }
    }

    fun CmdLiteralScope.tags() {
        literal("tags") {
            execSuspend {
                val tags = request { MarketplaceApi.getTags() }
                chat(regular(t("tags.list", variable(tags.joinToString(", ") { it.name }))))
            }
        }
    }

    private fun CmdI18n.printPage(response: PaginatedResponse<MarketplaceItem>) {
        if (response.items.isEmpty()) {
            chat(regular(t("list.noConfigs")))
            return
        }

        chat(
            highlight(t("list.header", response.pagination.current, response.pagination.pages)),
            metadata = MessageMetadata(prefix = false)
        )
        response.items.forEach { chat(row(it), metadata = MessageMetadata(prefix = false)) }
    }

    private fun CmdI18n.row(item: MarketplaceItem) = regular("")
        .append(variable(item.name))
        .apply {
            if (ConfigTracker.state != ConfigTracker.State.NONE && ConfigTracker.itemId == item.id) {
                append(" *".asPlainText(ChatFormatting.GREEN))
            }
            item.author?.let { append(regular(" ")).append(regular(t("list.by", variable(it)))) }
            append(regular(" | "))
            append("✔ ${item.recentWorks}".asPlainText(ChatFormatting.GREEN))
            append(regular(" "))
            append("✘ ${item.recentFails}".asPlainText(ChatFormatting.RED))
            item.targetServers?.takeIf { it.isNotEmpty() }?.let {
                append(regular(" | ${it.joinToString(", ")}"))
            }
        }
        .onClick(ClickEvent.SuggestCommand("${CommandManager.GlobalSettings.prefix}config load ${item.id}"))
        .onHover(HoverEvent.ShowText(regular(t("list.hover", variable(item.name)))))

}
