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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import java.net.InetSocketAddress
import java.util.*

/**
 * Proxy Manager
 */
object ProxyManager : Configurable("Proxies") {

    val proxies by value(name, TreeSet<Proxy>(), listType = ListValueType.Proxy)

    var currentProxy: Proxy? = null

    init {
        ConfigSystem.root(this)
    }

    fun setProxy(host: String, port: Int, username: String, password: String): String {
        ProxyManager.currentProxy = ProxyManager.Proxy(InetSocketAddress(host, port), if (username.isNotBlank()) ProxyManager.ProxyCredentials(username, password) else null)

        return "Successfully set proxy"
    }

    fun unsetProxy(): String {
        ProxyManager.currentProxy = null
        return "Successfully unset proxy"
    }

    // todo: hook into ChannelInitializer
    // val proxy = currentProxy
    //            if (proxy != null) {
    //                p.addFirst(
    //                    "proxy",
    //                    if (proxy.credentials != null) {
    //                        Socks5ProxyHandler(proxy.address, proxy.credentials.username, proxy.credentials.password)
    //                    } else {
    //                        Socks5ProxyHandler(proxy.address)
    //                    }
    //                )
    //            }

    data class Proxy(val address: InetSocketAddress, val credentials: ProxyCredentials?)

    data class ProxyCredentials(val username: String, val password: String)

}
