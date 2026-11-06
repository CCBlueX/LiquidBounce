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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.commands.client.config.ago
import net.ccbluex.liquidbounce.features.command.commands.client.config.header
import net.ccbluex.liquidbounce.features.command.commands.client.config.plain
import net.ccbluex.liquidbounce.features.command.commands.client.config.request
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.marketplaceItem
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.marketplaceItemSuggestions
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs.address
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.minecraft.ChatFormatting

object MarketplaceListRevisionsCommand {

    fun CmdLiteralScope.revisionsList() {
        literal("list") {
            argument("item", ClientStringArgumentType.string(), suggests = marketplaceItemSuggestions) { input ->
                execSuspend { ctx ->
                    val item = marketplaceItem(ctx.get(input))
                    val response = request { MarketplaceApi.getMarketplaceItemRevisions(item.id) }

                    val activeRevisions = response.items.filter { it.status != MarketplaceItemStatus.PENDING }

                    header(t("revisions.list.header", variable(item.address)))
                    if (activeRevisions.isEmpty()) {
                        chat(regular(t("revisions.list.noRevisions")), metadata = plain)
                        return@execSuspend
                    }

                    for (revision in activeRevisions) {
                        chat(
                            regular("⬥ ").withColor(ChatFormatting.BLUE)
                                .append(variable(revision.version))
                                .append(regular("  "))
                                .append(ago(revision.createdAt)),
                            metadata = plain
                        )
                    }
                }
            }
        }
    }

}
