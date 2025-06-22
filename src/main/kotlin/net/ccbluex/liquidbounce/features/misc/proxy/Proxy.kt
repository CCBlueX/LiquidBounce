/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import java.net.InetSocketAddress

/**
 * Contains serializable proxy data
 */
data class Proxy(
    val host: String,
    val port: Int,
    val credentials: Credentials?,
    val type: Type?,
    var forwardAuthentication: Boolean = false,
    var ipInfo: IpInfoApi.IpData? = null,
    var favorite: Boolean = false
) {

    enum class Type {
        HTTP,
        SOCKS5,
    }

    val address
        get() = InetSocketAddress(host, port)

    fun handler() = when (type ?: Type.SOCKS5) {
        Type.HTTP -> if (credentials == null) {
            HttpProxyHandler(address)
        } else {
            HttpProxyHandler(address, credentials.username, credentials.password)
        }
        Type.SOCKS5 -> if (credentials == null) {
            Socks5ProxyHandler(address)
        } else {
            Socks5ProxyHandler(address, credentials.username, credentials.password)
        }
    }

    class Credentials(val username: String, val password: String)

    companion object {
        @JvmStatic
        fun credentials(username: String, password: String): Credentials? {
            return if (username.isBlank() || password.isBlank()) {
                null
            } else {
                Credentials(username, password)
            }
        }
    }

}
