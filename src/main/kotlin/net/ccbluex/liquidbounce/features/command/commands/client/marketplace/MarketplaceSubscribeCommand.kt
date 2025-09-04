/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Subscribe to marketplace item
 */
object MarketplaceSubscribeCommand : Command.Factory {

    override fun createCommand() = CommandBuilder.begin("subscribe")
        .parameter(
            ParameterBuilder
                .begin<Int>("id")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .required()
                .build()
        )
        .suspendHandler {
            val id = args[0] as Int

            if (MarketplaceManager.isSubscribed(id)) {
                chat(regular(command.result("alreadySubscribed", variable(id.toString()))))
                return@suspendHandler
            }

            runCatching {
                // Verify the item exists and is not pending
                val item = MarketplaceApi.getMarketplaceItem(id)
                if (item.status != MarketplaceItemStatus.ACTIVE) {
                    throw CommandException(translation("liquidbounce.command.marketplace.error.itemPending"))
                }

                MarketplaceManager.subscribe(item)
                chat(regular(command.result("success", variable(id.toString()))))
            }.onFailure { e ->
                logger.error("Failed to subscribe to marketplace item", e)
                throw CommandException(
                    translation(
                        "liquidbounce.command.marketplace.error.installFailed",
                        id,
                        e.message ?: "Unknown error"
                    )
                )
            }
        }
        .build()

}
