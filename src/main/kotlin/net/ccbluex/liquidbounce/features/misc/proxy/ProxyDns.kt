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
package net.ccbluex.liquidbounce.features.misc.proxy

import com.google.common.net.InetAddresses
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.net.InetSocketAddress
import java.util.Optional

/**
 * Keeps server lookups off the local resolver while [ProxyManager.dnsProxy] is set.
 *
 * Hostnames stay unresolved for the proxy to resolve. SRV records, which a proxy cannot look up, go over DNS over HTTPS
 * through it.
 */
object ProxyDns {

    private val logger = clientLogger("ProxyDns")

    private const val DOH_URL = "https://cloudflare-dns.com/dns-query"
    private const val SRV = 33
    private const val DEFAULT_PORT = 25565

    private val ServerAddress.needsLookup
        get() = !host.equals("localhost", ignoreCase = true) && !InetAddresses.isInetAddress(host)

    @JvmStatic
    fun resolver(system: ServerAddressResolver) = ServerAddressResolver { address ->
        if (ProxyManager.dnsProxy == null || !address.needsLookup) {
            system.resolve(address)
        } else {
            Optional.of(UnresolvedServerAddress(InetSocketAddress.createUnresolved(address.host, address.port)))
        }
    }

    @JvmStatic
    fun redirectHandler(system: ServerRedirectHandler) = ServerRedirectHandler { address ->
        val proxy = ProxyManager.dnsProxy
        when {
            proxy == null || !address.needsLookup -> system.lookupRedirect(address)
            // Vanilla only looks up SRV records for the default port
            address.port != DEFAULT_PORT -> Optional.empty()
            else -> Optional.ofNullable(runCatching { lookupSrv(proxy, address.host) }.onFailure {
                logger.debug("SRV lookup for ${address.host} through the proxy failed", it)
            }.getOrNull())
        }
    }

    private fun lookupSrv(proxy: Proxy, host: String): ServerAddress? {
        val url = DOH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("name", "_minecraft._tcp.$host")
            .addQueryParameter("type", "SRV")
            .build()
        val request = Request.Builder().url(url).header("Accept", "application/dns-json").build()

        val answers = proxy.httpClient().newCall(request).execute().parse<JsonObject>()["Answer"]?.asJsonArray
        val record = answers?.map { it.asJsonObject }?.firstOrNull { it["type"].asInt == SRV } ?: return null

        // priority weight port target, as JNDI hands it to vanilla
        val (_, _, port, target) = record["data"].asString.split(' ', limit = 4)
        return ServerAddress(target, ServerAddress.parsePort(port))
    }

    private class UnresolvedServerAddress(private val address: InetSocketAddress) : ResolvedServerAddress {
        override fun getHostName(): String = address.hostString
        override fun getHostIp(): String = address.hostString
        override fun getPort() = address.port
        override fun asInetSocketAddress() = address
    }

}
