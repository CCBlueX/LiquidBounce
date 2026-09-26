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

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.NoCompatibleRevisionException
import net.ccbluex.liquidbounce.features.marketplace.installWithDependencies
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
                    this@subscribe.subscribe(ctx.get(input))
                }
            }
        }
    }

    private suspend fun CmdI18n.subscribe(input: String) {
        val item = marketplaceItem(input)
        val itemId = item.id

        if (MarketplaceManager.isSubscribed(itemId)) {
            chat(regular(t("subscribe.alreadySubscribed", variable(itemId.toString()))))
            return
        }

        val installed = runCatching {
            // An item named by its id can still be pending
            if (item.status != MarketplaceItemStatus.ACTIVE) {
                throw CommandException(t("error.itemPending"))
            }

            installedWith(item)
        }.getOrElse { e -> throw CommandException(failureText(e, itemId)) }

        chat(regular(t("subscribe.success", variable(itemId.toString()))))
        val needed = installed.filter { it.id != itemId }
        if (needed.isNotEmpty()) {
            chat(regular(t("subscribe.dependencies", variable(needed.joinToString(", ") { it.name }))))
        }
    }

    /**
     * What subscribing to [item] installed, after what it needs.
     */
    private suspend fun installedWith(item: MarketplaceItem): List<MarketplaceItem> {
        val (installed, unavailable) = installWithDependencies(item)
        if (installed.none { it.id == item.id }) {
            throw NoCompatibleRevisionException(unavailable.first())
        }
        return installed
    }

    private fun CmdI18n.failureText(e: Throwable, itemId: Int) = if (e is NoCompatibleRevisionException) {
        e.unavailable.text()
    } else {
        logger.error("Failed to subscribe to marketplace item", e)
        t("error.installFailed",
            itemId,
            e.message ?: "Unknown error"
        )
    }

}
