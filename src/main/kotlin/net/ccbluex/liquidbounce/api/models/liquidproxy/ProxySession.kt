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
 * One connection to a server through LiquidProxy.
 *
 * @param rootHost the server, as a name to show
 * @param exitCountry where the IP of the session is from
 * @param endedAt `null` while [connected]
 */
data class ProxySession(
    val id: String,
    val service: String,
    val locationCode: String,
    val player: String,
    val host: String,
    val rootHost: String,
    val ipType: String?,
    val exitCountry: String?,
    val startedAt: Instant,
    val lastSeenAt: Instant,
    val endedAt: Instant?,
    val connected: Boolean,
    private val stickyKey: String?,
    private val hostKey: String?,
) {

    /**
     * The IP this session was given, if it is one that LiquidProxy keeps.
     */
    val savedIp
        get() = stickyKey?.let { ProxySavedIp(service, locationCode, it, hostKey) }

}
