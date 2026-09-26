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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Ping and maintenance state of the LiquidProxy locations, as seen from this client.
 */
internal object LocationProbes {

    private const val STATUS_PORT = 80
    private const val TIMEOUT_MS = 2000
    private val INTERVAL = 30.seconds

    data class Probe(val latency: Int?, val maintenance: Boolean)

    private val probes = ConcurrentHashMap<String, Probe>()

    @Volatile
    private var probedAt = 0L

    operator fun get(location: ProxyLocation) = probes[location.code]

    /**
     * Probes every location, at most every [INTERVAL]. Results arrive in [get].
     */
    fun probe(locations: List<ProxyLocation>) {
        val now = System.currentTimeMillis()
        if (now - probedAt < INTERVAL.inWholeMilliseconds) {
            return
        }
        probedAt = now

        for (location in locations) {
            ioScope.launch {
                probes[location.code] = probe(location.address)
            }
        }
    }

    private suspend fun probe(host: String) = coroutineScope {
        // Every node answers its status page, and flags maintenance there
        val maintenance = async {
            runCatching {
                HttpClient.request("http://$host/", HttpMethod.GET).parse<NodeStatus>().service.maintenance
            }.getOrDefault(false)
        }
        val latency = runCatching { runInterruptible(Dispatchers.IO) { measureLatency(host) } }.getOrNull()
        Probe(latency, maintenance.await())
    }

    private fun measureLatency(host: String): Int {
        val address = InetSocketAddress(InetAddress.getByName(host), STATUS_PORT)
        Socket().use { socket ->
            val start = System.nanoTime()
            socket.connect(address, TIMEOUT_MS)
            return ((System.nanoTime() - start) / 1_000_000).toInt()
        }
    }

    private class NodeStatus(val service: Service) {
        class Service(val maintenance: Boolean)
    }

}
