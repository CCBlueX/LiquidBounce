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
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Unsubscribe from a marketplace item
 */
object MarketplaceUnsubscribeCommand {

    fun CmdLiteralScope.unsubscribe() {
        literal("unsubscribe") {
            argument(
                "id",
                IntegerArgumentType.integer(1),
                suggestions { MarketplaceManager.subscribedItems.map { it.id.toString() } },
            ) { id ->
                execSuspend { ctx ->
                    val itemId = ctx.get(id)

                    if (!MarketplaceManager.isSubscribed(itemId)) {
                        chat(regular(t("unsubscribe.notSubscribed", variable(itemId.toString()))))
                        return@execSuspend
                    }

                    MarketplaceManager.unsubscribe(itemId)
                    chat(regular(t("unsubscribe.success", variable(itemId.toString()))))
                }
            }
        }
    }

}
