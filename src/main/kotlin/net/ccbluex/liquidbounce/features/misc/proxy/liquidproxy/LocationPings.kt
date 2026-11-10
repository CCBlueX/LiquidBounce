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
package net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * How long this client takes to reach the LiquidProxy locations.
 */
internal object LocationPings {

    private const val TIMEOUT_MS = 1000

    /**
     * @return milliseconds by location code, with no entry for a location that can't be reached
     */
    suspend fun measure(locations: List<ProxyLocation>): Map<String, Int> = coroutineScope {
        locations
            .filter { !it.maintenance && LiquidProxy.SOCKS_PORT in it.ports }
            .map { location -> async { location.code to ping(location.host) } }
            .awaitAll()
            .mapNotNull { (code, ping) -> ping?.let { code to it } }
            .toMap()
    }

    private suspend fun ping(host: String): Int? = try {
        runInterruptible(Dispatchers.IO) {
            val address = InetSocketAddress(InetAddress.getByName(host), LiquidProxy.SOCKS_PORT)
            Socket().use { socket ->
                val start = System.nanoTime()
                socket.connect(address, TIMEOUT_MS)
                ((System.nanoTime() - start) / 1_000_000).toInt()
            }
        }
    } catch (_: IOException) {
        null
    }

}
