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
package net.ccbluex.liquidbounce.api.models.liquidproxy

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * A LiquidProxy subscription
 */
data class ProxySubscription(
    val level: Int,
    val status: Int,
    @SerializedName("auto_renew")
    val autoRenew: Boolean,
    @SerializedName("valid_until")
    val validUntil: String,
    val username: String,
    val password: String,
) {

    // The API sends UTC without an offset
    val expiresAt: Instant
        get() = LocalDateTime.parse(validUntil).toInstant(ZoneOffset.UTC)

    val isActive
        get() = status == 0 && expiresAt.isAfter(Instant.now())

}
