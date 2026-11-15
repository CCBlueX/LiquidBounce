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

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.API_EDITION
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceTag
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.utils.client.clientLogger

object MarketplaceConfigs {

    private val logger = clientLogger("MarketplaceConfigs")

    const val SHARE_CODE_PREFIX = "LB-"
    private const val INDEX_LIMIT = 50
    const val PAGE_LIMIT = 10

    /**
     * Top configs by score, for suggestions only.
     */
    @Volatile
    var index: List<MarketplaceItem> = emptyList()
        private set

    @Volatile
    var tags: List<MarketplaceTag> = emptyList()
        private set

    suspend fun refresh(): Boolean = runCatching {
        index = list(limit = INDEX_LIMIT).items
        tags = MarketplaceApi.getTags()
    }.onFailure { logger.error("Failed to load the marketplace config index", it) }.isSuccess

    @Suppress("LongParameterList")
    suspend fun list(
        page: Int = 1,
        limit: Int = PAGE_LIMIT,
        query: String? = null,
        tags: List<Int> = emptyList(),
        name: String? = null,
        author: String? = null,
        targetServer: String? = null,
        featured: Boolean? = null,
        sort: MarketplaceApi.Sort = MarketplaceApi.Sort.SCORE,
        session: OAuthSession? = null,
    ) = MarketplaceApi.getMarketplaceItems(
        page = page,
        limit = limit,
        query = query,
        type = MarketplaceItemType.CONFIG,
        branch = API_EDITION,
        filter = MarketplaceApi.Filter(
            name = name,
            author = author,
            featured = featured,
            tags = tags,
            targetServer = targetServer,
            sort = sort
        ),
        session = session
    )

    /**
     * [input] is a share code, an item id, `author/name` or a bare name. More than one result means the
     * bare name is ambiguous.
     */
    suspend fun find(input: String, session: OAuthSession? = null): List<MarketplaceItem> =
        findItems(input, listOf(MarketplaceItemType.CONFIG), session)

    /**
     * Like [find], for anything a config can depend on.
     */
    suspend fun findDependency(input: String): List<MarketplaceItem> = findItems(
        input,
        listOf(MarketplaceItemType.CONFIG, MarketplaceItemType.ADDON, MarketplaceItemType.SCRIPT)
    )

    /**
     * Like [find]. A bare name goes to the first of [types] that has it.
     */
    internal suspend fun findItems(
        input: String,
        types: List<MarketplaceItemType>,
        session: OAuthSession? = null,
    ) = runCatching {
        when {
            input.startsWith(SHARE_CODE_PREFIX, ignoreCase = true) ->
                listOf(MarketplaceApi.getMarketplaceItemByCode(input, session))
            input.toIntOrNull() != null -> listOf(MarketplaceApi.getMarketplaceItem(input.toInt(), session))
            else -> {
                val author = input.substringBefore('/', "").takeIf(String::isNotEmpty)
                val name = input.substringAfter('/')
                types.firstNotNullOfOrNull { findByName(it, name, author, session).takeIf(List<*>::isNotEmpty) }
                    .orEmpty()
            }
        }.filter { it.type in types }
    }.onFailure { logger.info("Nothing found for $input", it) }.getOrDefault(emptyList())

    private suspend fun findByName(type: MarketplaceItemType, name: String, author: String?, session: OAuthSession?) =
        MarketplaceApi.getMarketplaceItems(
            limit = PAGE_LIMIT,
            type = type,
            branch = API_EDITION.takeIf { type == MarketplaceItemType.CONFIG },
            filter = MarketplaceApi.Filter(name = name, author = author, sort = MarketplaceApi.Sort.SCORE),
            session = session,
        ).items

    /**
     * `author/name`, or the name while the author is unknown.
     */
    val MarketplaceItem.address: String
        get() = author?.let { "$it/$name" } ?: name

    suspend fun findForServer(address: String, onlyFeatured: Boolean): MarketplaceItem? =
        list(limit = 1, targetServer = address, featured = if (onlyFeatured) true else null).items.firstOrNull()

}
