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
package net.ccbluex.liquidbounce.api.models.marketplace

/**
 * A published item, or one its managers read or changed.
 *
 * @param dependencies what it needs itself
 * @param liveRevision the newest published revision, `null` while nothing is published
 * @param compatibleRevision add-ons: the newest revision that loads in this game, `null` when none does
 * @param recentReports configs: reports on the live revision in the last 30 days
 * @param requires everything it needs, a dependency before what needs it. Lists leave it out.
 * @param shareCode only told to its managers
 */
data class MarketplaceItem(
    val id: Int,
    val type: MarketplaceItemType,
    val name: String,
    val owner: MarketplaceAuthor,
    val edition: String,
    val description: String,
    val createdAt: String,
    val sourceUrl: String? = null,
    val communityEditable: Boolean = false,
    val thumbnailPid: String? = null,
    val featured: Boolean = false,
    val visibility: MarketplaceItemVisibility = MarketplaceItemVisibility.PUBLIC,
    val downloads: Int = 0,
    val score: Double = 0.0,
    val rating: MarketplaceRating? = null,
    val tags: List<MarketplaceTag> = emptyList(),
    val targetServers: List<String> = emptyList(),
    val forkedFrom: MarketplaceItemRef? = null,
    val contributors: List<MarketplaceAuthor> = emptyList(),
    val dependencies: List<MarketplaceItemRef> = emptyList(),
    val liveRevision: MarketplaceItemRevision? = null,
    val compatibleRevision: MarketplaceItemRevision? = null,
    val recentReports: MarketplaceReportCounts? = null,
    val requires: List<MarketplaceItem>? = null,
    val shareCode: String? = null
)

/**
 * @param nickname the account name as the marketplace last saw it
 */
data class MarketplaceAuthor(
    val uid: String,
    val nickname: String? = null
)

/**
 * Enough of an item to name it.
 */
data class MarketplaceItemRef(
    val id: Int,
    val type: MarketplaceItemType,
    val name: String,
    val owner: MarketplaceAuthor
)

data class MarketplaceRating(val average: Double, val count: Int)

data class MarketplaceReportCounts(val works: Int, val fails: Int)
