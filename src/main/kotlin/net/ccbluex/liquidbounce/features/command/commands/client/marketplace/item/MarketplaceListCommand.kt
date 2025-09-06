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
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent

/**
 * List marketplace items
 */
@Suppress("CognitiveComplexMethod")
fun marketplaceListCommand() = buildCommand("list") {

    val type = addParam {
        enumChoice<MarketplaceItemType>("type") { it.isListable }
            .required()
    }

    val page = addParam("page") {
        verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
            .optional(1)
    }

    val featured = addParam("featured") {
        verifiedBy(ParameterBuilder.BOOLEAN_VALIDATOR)
            .optional(false)
    }

    suspendHandler {
        val type = type.cast()
        val page = page.cast()
        val featured = featured.cast()

        val response = MarketplaceApi.getMarketplaceItems(page, 10, type = type, featured = featured)

        if (response.items.isEmpty()) {
            chat(regular(command.result("noItems")))
            return@suspendHandler
        }

        chat(regular(command.result("header",
            variable(page.toString()),
            variable(response.pagination.pages.toString())
        )))

        for (item in response.items) {
            val isSubscribed = MarketplaceManager.isSubscribed(item.id)
            val action = if (isSubscribed) "unsubscribe" else "subscribe"
            chat(
                regular(
                    command.result(
                        "item",
                        variable(item.id.toString()),
                        variable("${item.name}${if (isSubscribed) "*" else ""}"),
                        variable(item.type.toString().lowercase()),
                        variable(if (item.featured) "★" else "")
                    ).onClick(
                        ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            ".marketplace $action ${item.id}"
                        )
                    ).onHover(
                        HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            variable(command.result("hover", variable(action), item.id))
                        )
                    )
                )
            )
        }
    }

}
