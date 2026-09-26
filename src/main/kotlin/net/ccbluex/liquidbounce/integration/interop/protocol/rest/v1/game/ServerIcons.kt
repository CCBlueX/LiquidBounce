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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.services.servermedia.ServerMediaApi
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain
import java.util.Base64
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Icons of Minecraft servers, as image URLs for the theme.
 *
 * The icon from server-media comes first, as it is usually a nicer version of the server's favicon.
 * Otherwise, the favicon of the server from the player's server list is used.
 */
object ServerIcons {

    private val RETRY_DELAY = 5.minutes

    private val logger = clientLogger("ServerIcons")

    /**
     * Returns the icon of the server [address] belongs to, or `null` when there is none.
     */
    suspend fun of(address: String): String? {
        val host = address.dropPort().lowercase()
        return serverMediaIcon(host) ?: serverListFavicon(host)
    }

    private suspend fun serverMediaIcon(host: String): String? {
        val domains = serverMediaDomains() ?: return null
        val folder = domains[host] ?: domains[host.rootDomain()] ?: return null
        return ServerMediaApi.iconUrl(folder)
    }

    private suspend fun serverListFavicon(host: String): String? {
        val domain = host.rootDomain()
        val favicon = withContext(Dispatchers.Main) {
            ActiveServerList.serverList.servers
                .filter { it.ip.dropPort().rootDomain() == domain }
                .firstNotNullOfOrNull { it.iconBytes }
        } ?: return null

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(favicon)
    }

    // The server-media index, which maps domains to the folder of their server. It is loaded once;
    // after a failed attempt, we wait for RETRY_DELAY before trying again.
    private val indexLock = Mutex()
    private var domains: Map<String, String>? = null
    private var retryAt: TimeMark? = null

    private suspend fun serverMediaDomains(): Map<String, String>? = indexLock.withLock {
        domains?.let { return it }
        if (retryAt?.hasNotPassedNow() == true) {
            return null
        }

        try {
            ServerMediaApi.getIndex().domains.also { domains = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to load the server-media index", e)
            retryAt = TimeSource.Monotonic.markNow() + RETRY_DELAY
            null
        }
    }

}
