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

import com.google.common.net.HostAndPort
import com.google.common.net.InetAddresses
import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import okhttp3.Credentials as HttpCredentials

/**
 * Contains serializable proxy data
 */
data class Proxy(
    val host: String,
    val port: Int,
    val credentials: Credentials?,
    val type: Type?,
    var forwardAuthentication: Boolean = false,
    var proxyResourcePacks: Boolean = false,
    var proxyDns: Boolean = false,
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

    /**
     * An HTTP client that connects through this proxy and leaves hostnames for it to resolve
     */
    fun httpClient(): OkHttpClient {
        val type = type ?: Type.SOCKS5
        if (type == Type.SOCKS5 && credentials != null) {
            ProxyAuthenticator.install()
        }

        val javaProxyType = when (type) {
            Type.HTTP -> java.net.Proxy.Type.HTTP
            Type.SOCKS5 -> java.net.Proxy.Type.SOCKS
        }
        return HttpClient.client.newBuilder()
            .cache(null)
            .proxy(java.net.Proxy(javaProxyType, address))
            .proxyAuthenticator { _, response ->
                if (credentials == null || response.request.header("Proxy-Authorization") != null) {
                    null
                } else {
                    val authorization = HttpCredentials.basic(credentials.username, credentials.password)
                    response.request.newBuilder().header("Proxy-Authorization", authorization).build()
                }
            }
            .build()
    }

    class Credentials(val username: String, val password: String)

    companion object {

        @JvmField
        val NONE = Proxy("", 0, null, Type.SOCKS5)

        /**
         * Parse a proxy string into a [Proxy] object.
         *
         * Detects the proxy type based on the prefix of the string.
         * - `http://` for HTTP proxy
         * - `socks5://` or `socks5h://` for SOCKS5 proxy
         *
         * Accepts the following formats:
         * - `hostname:port:username:password`
         * - `username:password:hostname:port`
         * - `username:password@hostname:port`
         *
         * IPv6 hosts must be enclosed in brackets, for example `[2001:db8::1]:1080`.
         */
        fun parse(text: String): Proxy {
            val proxyType = when {
                text.startsWith("http://") -> Type.HTTP
                text.startsWith("socks5://") || text.startsWith("socks5h://") -> Type.SOCKS5
                else -> Type.SOCKS5
            }

            val proxyText = text.substringAfter("://")
            return when {
                '@' in proxyText -> {
                    val credentials = proxyText.substringBeforeLast('@').split(':', limit = 2)
                    require(credentials.size == 2) { "Proxy credentials must be username:password" }
                    val address = parseAddress(proxyText.substringAfterLast('@'))
                    Proxy(address.host, address.port, credentials(credentials[0], credentials[1]), proxyType)
                }

                else -> {
                    require(!InetAddresses.isInetAddress(proxyText)) { "IPv6 proxy hosts must be bracketed with a port" }
                    val parts = splitFields(proxyText)

                    when {
                        parts.size == 2 -> {
                            val address = parseAddress(proxyText)
                            Proxy(address.host, address.port, null, proxyType)
                        }

                        // host:port:username:password
                        parts.size == 4 && parts[1].isValidPort() -> {
                            val address = parseAddress("${parts[0]}:${parts[1]}")
                            Proxy(address.host, address.port, credentials(parts[2], parts[3]), proxyType)
                        }

                        // username:password:host:port
                        parts.size == 4 && parts[3].isValidPort() -> {
                            val address = parseAddress("${parts[2]}:${parts[3]}")
                            Proxy(address.host, address.port, credentials(parts[0], parts[1]), proxyType)
                        }

                        else -> throw IllegalArgumentException("Invalid proxy format: expected a host and valid port")
                    }
                }
            }
        }

        private fun parseAddress(text: String): HostAndPort {
            val address = HostAndPort.fromString(text).requireBracketsForIPv6()
            require(address.host.isNotBlank()) { "Proxy host must not be empty" }
            require(address.hasPort() && address.port in 1..65535) { "Proxy port must be in 1..65535" }
            return address
        }

        /** Split legacy formats without treating the colons inside an IPv6 host as separators. */
        private fun splitFields(text: String): List<String> {
            val fields = ArrayList<String>()
            var start = 0
            var bracketed = false
            for ((index, char) in text.withIndex()) {
                when (char) {
                    '[' -> {
                        require(!bracketed) { "Invalid proxy host brackets" }
                        bracketed = true
                    }
                    ']' -> {
                        require(bracketed) { "Invalid proxy host brackets" }
                        bracketed = false
                    }
                    ':' -> if (!bracketed) {
                        fields.add(text.substring(start, index))
                        start = index + 1
                    }
                }
            }
            require(!bracketed) { "Unclosed proxy host bracket" }
            fields.add(text.substring(start))
            return fields
        }

        private fun String.isValidPort(): Boolean {
            val port = toIntOrNull() ?: return false
            return port in 1..65535
        }

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
