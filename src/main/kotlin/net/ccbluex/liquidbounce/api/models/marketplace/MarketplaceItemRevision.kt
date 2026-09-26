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

data class MarketplaceItemRevision(
    val id: Int,
    @SerializedName("item_id")
    val itemId: Int,
    val version: String,
    @SerializedName("file_pid")
    val filePid: String,
    val changelog: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val status: MarketplaceItemStatus,
    @SerializedName("includes_binds")
    val includesBinds: Boolean? = null,
    /**
     * The LiquidBounce versions an add-on revision works with, `null` when no build is known to.
     */
    val liquidbounce: LiquidBounceRange? = null
)

data class LiquidBounceRange(val min: String, val max: String) {
    override fun toString() = if (min == max) "LiquidBounce v$min" else "LiquidBounce v$min - $max"
}
