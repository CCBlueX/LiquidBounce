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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * List marketplace item revisions
 */
object MarketplaceListRevisionsCommand : Command.Factory {

    override fun createCommand() = CommandBuilder.begin("list")
        .parameter(
            ParameterBuilder
                .begin<Int>("id")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .required()
                .build()
        )
        .suspendHandler {
            val id = args[0] as Int

            val response = MarketplaceApi.getMarketplaceItemRevisions(id)

            // Filter out pending revisions
            val activeRevisions = response.items.filter { it.status != MarketplaceItemStatus.PENDING }

            if (activeRevisions.isEmpty()) {
                chat(regular(command.result("noRevisions")))
                return@suspendHandler
            }

            chat(regular(command.result("header", variable(id.toString()))))

            for (revision in activeRevisions) {
                chat(
                    regular(
                        command.result(
                            "revision",
                            variable(revision.version),
                            variable(revision.createdAt)
                        )
                    )
                )
            }
        }
        .build()
}
