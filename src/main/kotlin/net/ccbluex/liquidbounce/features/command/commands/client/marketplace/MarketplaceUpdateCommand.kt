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
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Subscribe to marketplace item
 */
object MarketplaceUpdateCommand : Command.Factory {

    override fun createCommand() = CommandBuilder.begin("update")
        .parameter(
            ParameterBuilder
                .begin<Int>("id")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .autocompletedFrom {
                    MarketplaceManager.subscribedItems.map { item ->
                        item.id.toString()
                    }
                }
                .optional()
                .build()
        )
        .suspendHandler {
            val id = args.getOrNull(0) as Int?

            if (id != null) {
                val item = MarketplaceManager.getItem(id)
                    ?: throw CommandException(
                        translation(
                            "command.marketplace.error.itemNotFound",
                            variable(id.toString())
                        )
                    )

                MarketplaceManager.update(item, command = command)
            } else {
                if (MarketplaceManager.subscribedItems.isEmpty()) {
                    throw CommandException(command.result("noSubscribedItems"))
                }

                chat(regular(command.result("updatingAll")))
                MarketplaceManager.updateAll(command = command)
                chat(regular(command.result("updatedAll")))
            }
        }
        .build()

}
