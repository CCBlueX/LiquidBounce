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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item

import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Create marketplace item
 */
object MarketplaceCreateItemCommand : CommandFactory {

    override fun createCommand() = CommandBuilder.begin("create")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<String>("type")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { begin, _ ->
                    MarketplaceItemType.entries.map { it.name.lowercase() }
                        .filter { it.startsWith(begin, ignoreCase = true) }
                }
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<String>("description")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .vararg()
                .required()
                .build()
        )
        .suspendHandler { command, args ->
            val clientAccount = ClientAccountManager.clientAccount
            if (clientAccount == EMPTY_ACCOUNT) {
                throw CommandException(translation("liquidbounce.command.marketplace.error.notLoggedIn"))
            }

            val name = args[0] as String
            val typeStr = args[1] as String
            val description = (args[2] as Array<*>).joinToString(" ")

            val type = try {
                MarketplaceItemType.valueOf(typeStr.uppercase())
            } catch (_: IllegalArgumentException) {
                throw CommandException(translation("liquidbounce.command.marketplace.error.invalidItemType"))
            }

            val response = MarketplaceApi.createMarketplaceItem(
                clientAccount.takeSession(),
                name,
                type,
                description
            )

            chat(
                regular(
                    command.result(
                        "success",
                        variable(response.id.toString()),
                        variable(response.name)
                    )
                )
            )
        }
        .build()
}
