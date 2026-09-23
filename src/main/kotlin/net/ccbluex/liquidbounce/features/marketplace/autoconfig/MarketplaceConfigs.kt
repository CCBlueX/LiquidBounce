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
package net.ccbluex.liquidbounce.features.marketplace.autoconfig

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.API_BRANCH
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.utils.client.clientLogger

/**
 * Resolves names, share codes and server addresses to marketplace configs.
 */
object MarketplaceConfigs {

    private val logger = clientLogger("MarketplaceConfigs")

    const val SHARE_CODE_PREFIX = "LB-"
    private const val INDEX_LIMIT = 50
    const val PAGE_LIMIT = 10

    /**
     * Top configs by score. Only feeds suggestions; every load resolves against the API.
     */
    @Volatile
    var index: List<MarketplaceItem> = emptyList()
        private set

    suspend fun refresh(): Boolean = runCatching {
        index = list(limit = INDEX_LIMIT).items
    }.onFailure { logger.error("Failed to load the marketplace config index", it) }.isSuccess

    suspend fun list(
        page: Int = 1,
        limit: Int = PAGE_LIMIT,
        query: String? = null,
        tags: List<Int> = emptyList(),
        name: String? = null,
        targetServer: String? = null,
    ) = MarketplaceApi.getMarketplaceItems(
        page = page,
        limit = limit,
        query = query,
        type = MarketplaceItemType.CONFIG,
        branch = API_BRANCH,
        filter = MarketplaceApi.Filter(
            name = name,
            tags = tags,
            targetServer = targetServer,
            sort = MarketplaceApi.Sort.SCORE
        )
    )

    /**
     * Accepts a share code, an item id or an exact name. Among configs of the same name,
     * the best ranked one wins.
     */
    suspend fun find(input: String): MarketplaceItem? = runCatching {
        when {
            input.startsWith(SHARE_CODE_PREFIX, ignoreCase = true) ->
                MarketplaceApi.getMarketplaceItemByCode(input)
            input.toIntOrNull() != null ->
                MarketplaceApi.getMarketplaceItem(input.toInt()).takeIf { it.type == MarketplaceItemType.CONFIG }
            else -> list(limit = 1, name = input).items.firstOrNull()
        }
    }.onFailure { logger.info("No config found for $input", it) }.getOrNull()

    suspend fun findForServer(address: String): MarketplaceItem? =
        list(limit = 1, targetServer = address).items.firstOrNull()

}
