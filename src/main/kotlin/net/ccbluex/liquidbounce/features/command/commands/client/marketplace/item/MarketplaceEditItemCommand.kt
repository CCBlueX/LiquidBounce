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

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.enumChoice
import net.ccbluex.liquidbounce.features.command.dsl.addParam
import net.ccbluex.liquidbounce.features.command.dsl.buildCommand
import net.ccbluex.liquidbounce.features.command.dsl.cast
import net.ccbluex.liquidbounce.features.command.dsl.castVararg
import net.ccbluex.liquidbounce.features.command.preset.accountOrException
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Edit marketplace item
 */
fun marketplaceEditItemCommand() = buildCommand("edit") {

    val id = addParam("id") {
        verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
            .required()
    }

    val name = addParam("name") {
        verifiedBy(ParameterBuilder.STRING_VALIDATOR)
            .required()
    }

    val type = addParam {
        enumChoice<MarketplaceItemType>("type") { it.isListable }
            .required()
    }

    val description = addParam("description") {
        verifiedBy(ParameterBuilder.STRING_VALIDATOR)
            .required()
            .vararg()
    }

    suspendHandler {
        val clientAccount = ClientAccountManager.accountOrException()

        val id = id.cast()
        val name = name.cast()
        val type = type.cast()
        val description = description.castVararg().joinToString(" ")

        val response = MarketplaceApi.updateMarketplaceItem(
            clientAccount.takeSession(),
            id,
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

}
