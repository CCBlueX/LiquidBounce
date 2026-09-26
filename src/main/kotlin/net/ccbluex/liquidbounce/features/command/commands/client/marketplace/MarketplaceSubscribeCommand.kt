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

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.NoCompatibleRevisionException
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Subscribe to marketplace item
 */
object MarketplaceSubscribeCommand {

    fun CmdLiteralScope.subscribe() {
        literal("subscribe") {
            argument("item", ClientStringArgumentType.string(), suggests = subscribableSuggestions) { input ->
                execSuspend { ctx ->
                    val item = marketplaceItem(ctx.get(input))
                    val itemId = item.id

                    if (MarketplaceManager.isSubscribed(itemId)) {
                        chat(regular(t("subscribe.alreadySubscribed", variable(itemId.toString()))))
                        return@execSuspend
                    }

                    runCatching {
                        // An item named by its id can still be pending
                        if (item.status != MarketplaceItemStatus.ACTIVE) {
                            throw CommandException(t("error.itemPending"))
                        }

                        MarketplaceManager.subscribe(item)
                        chat(regular(t("subscribe.success", variable(itemId.toString()))))
                    }.onFailure { e ->
                        val text = if (e is NoCompatibleRevisionException) {
                            e.unavailable.text()
                        } else {
                            logger.error("Failed to subscribe to marketplace item", e)
                            t("error.installFailed",
                                itemId,
                                e.message ?: "Unknown error"
                            )
                        }
                        throw CommandException(text)
                    }
                }
            }
        }
    }

}
