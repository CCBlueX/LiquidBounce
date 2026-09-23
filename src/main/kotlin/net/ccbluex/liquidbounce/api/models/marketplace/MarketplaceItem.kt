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

import com.google.gson.annotations.SerializedName

data class MarketplaceItem(
    val id: Int,
    val uid: String,
    val type: MarketplaceItemType,
    val name: String,
    val branch: String,
    val description: String,
    @SerializedName("thumbnail_pid")
    val thumbnailPid: String?,
    val featured: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    val status: MarketplaceItemStatus,
    val author: String? = null,
    @SerializedName("live_revision_id")
    val liveRevisionId: Int? = null,
    val tags: List<MarketplaceTag>? = null,
    @SerializedName("target_servers")
    val targetServers: List<String>? = null,
    @SerializedName("forked_from_item_id")
    val forkedFromItemId: Int? = null,
    @SerializedName("forked_from_revision_id")
    val forkedFromRevisionId: Int? = null,
    val downloads: Int = 0,
    val score: Double = 0.0,
    val visibility: MarketplaceItemVisibility? = null,
    @SerializedName("share_code")
    val shareCode: String? = null,
    @SerializedName("includes_binds")
    val includesBinds: Boolean? = null,
    @SerializedName("recent_works")
    val recentWorks: Int = 0,
    @SerializedName("recent_fails")
    val recentFails: Int = 0
)
