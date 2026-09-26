/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of
 * the License, or (at your option) any later version.
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

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item.MarketplaceListCommand
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions.MarketplaceRevisionsCommand

/**
 * Marketplace command
 *
 * Allows interacting with the LiquidBounce Marketplace
 */
object CommandMarketplace : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("marketplace") {
            with(MarketplaceListCommand) { list() }
            with(MarketplaceSearchCommand) { search() }
            with(MarketplaceSubscribeCommand) { subscribe() }
            with(MarketplaceUnsubscribeCommand) { unsubscribe() }
            with(MarketplaceUpdateCommand) { update() }
            with(MarketplaceRevisionsCommand) { revisions() }
        }
    }

}
