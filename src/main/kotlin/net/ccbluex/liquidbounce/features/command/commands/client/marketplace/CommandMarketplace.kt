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

import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item.MarketplaceListCommand
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions.MarketplaceRevisionsCommand

/**
 * Marketplace command
 *
 * Allows interacting with the LiquidBounce Marketplace
 */
object CommandMarketplace : CommandFactory {

    override fun createCommand() = CommandBuilder.begin("marketplace")
        .hub()
        .subcommand(MarketplaceListCommand.createCommand())
        .subcommand(MarketplaceSearchCommand.createCommand())
        .subcommand(MarketplaceSubscribeCommand.createCommand())
        .subcommand(MarketplaceUnsubscribeCommand.createCommand())
        .subcommand(MarketplaceUpdateCommand.createCommand())
        .subcommand(MarketplaceRevisionsCommand.createCommand())
        // Editing items is disabled until proven stable
        // .subcommand(MarketplaceItemCommand.createCommand())
        .build()

}
