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

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain

/**
 * Icons of the servers configs target, from CCBlueX's mirror of LabyMod's server-media.
 */
object ServerIcons {

    private const val BASE_URL = "https://server-media.liquidbounce.net"
    private const val RETRY_AFTER_MS = 5 * 60 * 1000L

    private class Server(@SerializedName("direct_ip") val directIp: String?, val wildcards: List<String>?)

    private class Index(val servers: Map<String, Server>)

    private val logger = clientLogger("ServerIcons")

    private val mutex = Mutex()

    // Root domain to the server's folder.
    @Volatile
    private var folders: Map<String, String>? = null

    private var failedAt = 0L

    /**
     * The icon of the server [address] belongs to, `null` when server-media has none.
     */
    suspend fun of(address: String): String? =
        folders()[address.dropPort().rootDomain()]?.let { "$BASE_URL/minecraft_servers/$it/icon.png" }

    private suspend fun folders(): Map<String, String> = folders ?: mutex.withLock {
        folders ?: if (System.currentTimeMillis() - failedAt < RETRY_AFTER_MS) emptyMap() else fetch()
    }

    // Wildcards read `%.example.net`.
    private suspend fun fetch(): Map<String, String> = try {
        val index = HttpClient.request("$BASE_URL/index.json", HttpMethod.GET).parse<Index>()
        val folders = HashMap<String, String>()
        for ((folder, server) in index.servers) {
            for (address in listOfNotNull(server.directIp) + server.wildcards.orEmpty()) {
                folders.putIfAbsent(address.removePrefix("%.").dropPort().rootDomain(), folder)
            }
        }
        folders.also { this.folders = it }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn("Failed to load the server-media index", e)
        failedAt = System.currentTimeMillis()
        emptyMap()
    }

}
