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

data class MarketplaceTag(
    val id: Int,
    val name: String
)

data class MarketplaceConfigReport(
    val id: Int,
    @SerializedName("revision_id")
    val revisionId: Int,
    val works: Boolean,
    @SerializedName("client_version")
    val clientVersion: String?,
    @SerializedName("server_address")
    val serverAddress: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class MarketplaceConfigReportSummary(
    @SerializedName("revision_id")
    val revisionId: Int,
    val works: Int,
    val fails: Int
)
