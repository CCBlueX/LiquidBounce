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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
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
            argument("id", IntegerArgumentType.integer(1)) { id ->
                execSuspend { ctx ->
                    val itemId = ctx.get(id)

                    if (MarketplaceManager.isSubscribed(itemId)) {
                        chat(regular(t("subscribe.alreadySubscribed", variable(itemId.toString()))))
                        return@execSuspend
                    }

                    runCatching {
                        // Verify the item exists and is not pending
                        val item = MarketplaceApi.getMarketplaceItem(itemId)
                        if (item.status != MarketplaceItemStatus.ACTIVE) {
                            throw CommandException(t("error.itemPending"))
                        }

                        MarketplaceManager.subscribe(item)
                        chat(regular(t("subscribe.success", variable(itemId.toString()))))
                    }.onFailure { e ->
                        logger.error("Failed to subscribe to marketplace item", e)
                        throw CommandException(
                            t("error.installFailed",
                                itemId,
                                e.message ?: "Unknown error"
                            )
                        )
                    }
                }
            }
        }
    }

}
