/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.misc

import io.netty.channel.ChannelPipeline
import io.netty.handler.proxy.Socks5ProxyHandler
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PipelineEvent
import net.ccbluex.liquidbounce.event.handler
import java.net.InetSocketAddress
import java.util.*

/**
 * Proxy Manager
 *
 * Only supports SOCKS5 proxies.
 */
object ProxyManager : Configurable("Proxies"), Listenable {

    // List of proxies. Not used yet.
    val proxies by value(name, TreeSet<Proxy>(), listType = ListValueType.Proxy)

    /**
     * The proxy that is set in the current session and used for all server connections
     */
    var currentProxy: Proxy? = null

    init {
        ConfigSystem.root(this)
    }

    fun setProxy(host: String, port: Int, username: String, password: String): String {
        currentProxy = Proxy(InetSocketAddress(host, port), if (username.isNotBlank()) ProxyCredentials(username, password) else null)

        // Refreshes local IP info when proxy is set
        IpInfoApi.refreshLocalIpInfo()
        return "Successfully set proxy"
    }

    fun unsetProxy(): String {
        currentProxy = null

        // Refreshes local IP info when proxy is unset
        IpInfoApi.refreshLocalIpInfo()
        return "Successfully unset proxy"
    }

    /**
     * Adds a SOCKS5 netty proxy handler to the pipeline when a proxy is set
     *
     * @see Socks5ProxyHandler
     * @see PipelineEvent
     */
    val pipelineHandler = handler<PipelineEvent> {
        val pipeline = it.channelPipeline
        insertProxyHandler(pipeline)
    }

    fun insertProxyHandler(pipeline: ChannelPipeline) {
        currentProxy?.run {
            pipeline.addFirst(
                "proxy",
                if (credentials != null) {
                    Socks5ProxyHandler(address, credentials.username, credentials.password)
                } else {
                    Socks5ProxyHandler(address)
                }
            )
        }
    }

    /**
     * Contains serializable proxy data
     */
    data class Proxy(val address: InetSocketAddress, val credentials: ProxyCredentials?)

    /**
     * Contains serializable proxy credentials
     */
    data class ProxyCredentials(val username: String, val password: String)

}
