/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.IpInfo
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.api.IpInfoApi.requestIpInfo
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import net.ccbluex.liquidbounce.config.ValueType
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PipelineEvent
import net.ccbluex.liquidbounce.event.events.ProxyAdditionResultEvent
import net.ccbluex.liquidbounce.event.events.ProxyCheckResultEvent
import net.ccbluex.liquidbounce.event.events.ProxyEditResultEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.ProxyManager.Proxy.Companion.NO_PROXY
import net.ccbluex.liquidbounce.features.misc.ProxyManager.ProxyCredentials.Companion.credentialsFromUserInput
import java.net.InetSocketAddress

/**
 * Proxy Manager
 *
 * Only supports SOCKS5 proxies.
 */
object ProxyManager : Configurable("proxy"), Listenable {

    var proxy by value("selectedProxy", NO_PROXY, valueType = ValueType.PROXY)
    val proxies by value(name, mutableListOf<Proxy>(), listType = ListValueType.Proxy)

    /**
     * The proxy that is set in the current session and used for all server connections
     */
    val currentProxy
        get() = proxy.takeIf { it.host.isNotBlank() }

    init {
        ConfigSystem.root(this)
    }

    fun addProxy(host: String, port: Int, username: String = "", password: String = "") {
        Proxy(host, port, credentialsFromUserInput(username, password)).checkProxy(
            success = { proxy ->
                LiquidBounce.logger.info("Added proxy [${proxy.host}:${proxy.port}]")
                proxies.add(proxy)
                ConfigSystem.storeConfigurable(this)

                EventManager.callEvent(ProxyAdditionResultEvent(proxy = proxy))
            },
            failure = {
                LiquidBounce.logger.error("Failed to check proxy", it)

                EventManager.callEvent(ProxyAdditionResultEvent(error = it.message ?: "Unknown error"))
            }
        )
    }

    fun editProxy(index: Int, host: String, port: Int, username: String = "", password: String = "") {
        Proxy(host, port, credentialsFromUserInput(username, password)).checkProxy(
            success = { newProxy ->
                val isConnected = proxy == proxies[index]

                LiquidBounce.logger.info("Edited proxy [${proxy.host}:${proxy.port}]")
                proxies[index] = newProxy
                ConfigSystem.storeConfigurable(this)

                EventManager.callEvent(ProxyEditResultEvent(proxy = proxy))

                if (isConnected) {
                    setProxy(index)
                }
            },
            failure = {
                LiquidBounce.logger.error("Failed to check proxy", it)

                EventManager.callEvent(ProxyEditResultEvent(error = it.message ?: "Unknown error"))
            }
        )
    }

    fun checkProxy(index: Int) {
        val proxy = proxies.getOrNull(index) ?: error("Invalid proxy index")
        proxy.checkProxy(
            success = { proxy ->
                LiquidBounce.logger.info("Checked proxy [${proxy.host}:${proxy.port}]")
                ConfigSystem.storeConfigurable(this)

                EventManager.callEvent(ProxyCheckResultEvent(proxy = proxy))
            },
            failure = {
                LiquidBounce.logger.error("Failed to check proxy", it)
                EventManager.callEvent(ProxyCheckResultEvent(proxy = proxy, error = it.message ?: "Unknown error"))
            }
        )
    }

    fun removeProxy(index: Int) {
        val proxy = proxies.removeAt(index)
        if (proxy == currentProxy) {
            unsetProxy()
        }

        ConfigSystem.storeConfigurable(this)
    }

    fun setProxy(index: Int) {
        proxy = proxies[index]
        sync()
    }

    fun unsetProxy() {
        proxy = NO_PROXY
        sync()
    }

    fun favoriteProxy(index: Int) {
        val proxy = proxies[index]
        proxy.favorite = true
        ConfigSystem.storeConfigurable(this)
    }

    fun unfavoriteProxy(index: Int) {
        val proxy = proxies[index]
        proxy.favorite = false
        ConfigSystem.storeConfigurable(this)
    }

    private fun sync() {
        ConfigSystem.storeConfigurable(this)
        IpInfoApi.refreshLocalIpInfo()
    }

    /**
     * Adds a SOCKS5 netty proxy handler to the pipeline when a proxy is set
     *
     * @see Socks5ProxyHandler
     * @see PipelineEvent
     */
    val pipelineHandler = handler<PipelineEvent> {
        val pipeline = it.channelPipeline

        insertProxyHandler(currentProxy, pipeline)
    }

    fun insertProxyHandler(proxy: Proxy? = currentProxy, pipeline: ChannelPipeline) {
        proxy?.run {
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
    data class Proxy(
        val host: String,
        val port: Int,
        val credentials: ProxyCredentials?,
        var ipInfo: IpInfo? = null,
        var favorite: Boolean = false
    ) {
        val address
            get() = InetSocketAddress(host, port)

        companion object {
            val NO_PROXY = Proxy("", 0, null)
        }

        fun checkProxy(success: (Proxy) -> Unit, failure: (Throwable) -> Unit) =
            requestIpInfo(proxy = this, success = { ipInfo ->
                LiquidBounce.logger.info("IP Info [${ipInfo.country}, ${ipInfo.org}]")
                this.ipInfo = ipInfo

                success(this)
            }, failure = failure)

    }

    /**
     * Contains serializable proxy credentials
     */
    data class ProxyCredentials(val username: String, val password: String) {
        companion object {
            fun credentialsFromUserInput(username: String, password: String) =
                if (username.isNotBlank() && password.isNotBlank())
                    ProxyCredentials(username, password)
                else
                    null
        }
    }

}
