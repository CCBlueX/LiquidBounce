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

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item.marketplaceListCommand
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions.MarketplaceRevisionsCommand

/**
 * Marketplace command
 *
 * Allows interacting with the LiquidBounce Marketplace
 */
object CommandMarketplace : Command.Factory {

    override fun createCommand() = CommandBuilder.begin("marketplace")
        .hub()
        .subcommand(marketplaceListCommand())
        .subcommand(MarketplaceSearchCommand)
        .subcommand(MarketplaceSubscribeCommand)
        .subcommand(MarketplaceUnsubscribeCommand)
        .subcommand(MarketplaceUpdateCommand)
        .subcommand(MarketplaceRevisionsCommand)
        // Editing items is disabled until proven stable
        // .subcommand(MarketplaceItemCommand)
        .build()

}
