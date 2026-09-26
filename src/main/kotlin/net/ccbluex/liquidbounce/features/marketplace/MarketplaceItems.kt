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
package net.ccbluex.liquidbounce.features.marketplace

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.utils.client.clientLogger

/**
 * Finds the marketplace items one subscribes to, themes, add-ons and scripts, by name or share code.
 */
object MarketplaceItems {

    private val logger = clientLogger("MarketplaceItems")

    private const val INDEX_LIMIT = 50

    private val subscribable = MarketplaceItemType.entries.filter(MarketplaceItemType::isSubscribable)

    /**
     * Top items of every subscribable type by score. Only feeds suggestions; every subscribe looks the item up
     * in the API.
     */
    @Volatile
    var index: List<MarketplaceItem> = emptyList()
        private set

    suspend fun refresh(): Boolean = runCatching {
        // A listing without a type leaves add-ons out, so every type is asked for on its own
        index = subscribable.flatMap { type ->
            MarketplaceApi.getMarketplaceItems(
                limit = INDEX_LIMIT,
                type = type,
                filter = MarketplaceApi.Filter(sort = MarketplaceApi.Sort.SCORE)
            ).items
        }
    }.onFailure { logger.error("Failed to load the marketplace item index", it) }.isSuccess

    /**
     * Items [input] names: a share code, an item id, `author/name` or a bare name. More than one result means
     * the bare name is ambiguous.
     */
    suspend fun find(input: String): List<MarketplaceItem> = MarketplaceConfigs.lookup(input, subscribable)

}
