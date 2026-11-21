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

import java.time.Instant

/**
 * What an account may use of LiquidProxy.
 *
 * @param networks codes of the [ProxyNetwork]s it includes
 * @param accessUntil when it expires, or has expired
 */
data class ProxyEntitlement(
    val networks: List<String>,
    val accessUntil: Instant,
    private val state: String,
) {

    /**
     * Also while rate limited: joins are then refused for a while, each with a message that says so.
     */
    val isActive
        get() = state == "active" || state == "rate_limited"

    /**
     * When neither this nor [isActive], the account can't use LiquidProxy for another reason.
     */
    val isExpired
        get() = state == "expired"

}
