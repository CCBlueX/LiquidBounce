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
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.text.rootDomain
import java.util.Base64
import kotlin.time.Duration.Companion.minutes

/**
 * Icons of Minecraft servers, as image URLs for the theme.
 */
object ServerIcons {

    private val RETRY_AFTER = 5.minutes

    private val logger = clientLogger("ServerIcons")

    private val mutex = Mutex()

    @Volatile
    private var domains: Map<String, String>? = null

    private var failedAt = 0L

    /**
     * The icon of the server [address] belongs to: its icon from server-media, usually a nicer version of its
     * favicon, otherwise its favicon when it is in the server list. `null` when neither has one.
     */
    suspend fun of(address: String): String? {
        val host = address.dropPort().lowercase()
        val domain = host.rootDomain()
        return serverMedia()?.let { it[host] ?: it[domain] }?.let(ServerMediaApi::iconUrl) ?: favicon(domain)
    }

    private suspend fun favicon(domain: String) = withContext(Dispatchers.Minecraft) {
        val serverList = ActiveServerList.serverList
        (0 until serverList.size())
            .map(serverList::get)
            .firstOrNull { it.iconBytes != null && it.ip.dropPort().rootDomain() == domain }
            ?.let { "data:image/png;base64," + Base64.getEncoder().encodeToString(it.iconBytes) }
    }

    private suspend fun serverMedia(): Map<String, String>? = domains ?: mutex.withLock {
        domains ?: if (System.currentTimeMillis() - failedAt < RETRY_AFTER.inWholeMilliseconds) {
            null
        } else {
            try {
                ServerMediaApi.getIndex().domains.also { domains = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Failed to load the server-media index", e)
                failedAt = System.currentTimeMillis()
                null
            }
        }
    }

}
