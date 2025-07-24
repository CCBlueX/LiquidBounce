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

import io.netty.handler.proxy.Socks5ProxyHandler
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PipelineEvent
import net.ccbluex.liquidbounce.event.events.ProxyAdditionResultEvent
import net.ccbluex.liquidbounce.event.events.ProxyCheckResultEvent
import net.ccbluex.liquidbounce.event.events.ProxyEditResultEvent
import net.ccbluex.liquidbounce.event.handler

/**
 * Proxy Manager
 *
 * Only supports SOCKS5 proxies.
 */
object ProxyManager : Configurable("proxy"), EventListener {

    private val NO_PROXY = Proxy("", 0, null, Proxy.Type.SOCKS5)

    private var proxy by value("selectedProxy", NO_PROXY, valueType = ValueType.PROXY)
    internal val proxies by list(name, mutableListOf<Proxy>(), valueType = ValueType.PROXY)

    /**
     * The proxy that is set in the current session and used for all server connections
     */
    val currentProxy
        get() = proxy.takeIf { it.host.isNotBlank() }

    init {
        ConfigSystem.root(this)
    }

    @Suppress("LongParameterList")
    fun addProxy(
        host: String,
        port: Int,
        username: String = "",
        password: String = "",
        type: Proxy.Type = Proxy.Type.SOCKS5,
        forwardAuthentication: Boolean = false
    ) {
        Proxy(host, port, Proxy.credentials(username, password), type, forwardAuthentication).check(
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

    @Suppress("LongParameterList")
    fun editProxy(
        index: Int,
        host: String,
        port: Int,
        username: String = "",
        password: String = "",
        type: Proxy.Type = Proxy.Type.SOCKS5,
        forwardAuthentication: Boolean = false
    ) {
        Proxy(host, port, Proxy.credentials(username, password), type, forwardAuthentication).check(
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
        proxy.check(
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
        ConfigSystem.storeConfigurable(this)
    }

    fun unsetProxy() {
        proxy = NO_PROXY
        ConfigSystem.storeConfigurable(this)
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

    /**
     * Adds a SOCKS5 netty proxy handler to the pipeline when a proxy is set
     *
     * @see Socks5ProxyHandler
     * @see PipelineEvent
     */
    @Suppress("unused")
    private val pipelineHandler = handler<PipelineEvent> { event ->
        // If we are connecting to a local server, we don't need a proxy, as this would cause a connection error.
        if (event.local) {
            return@handler
        }

        val pipeline = event.channelPipeline

        // Only add the proxy handler if it's not already in the pipeline. If there is already a proxy handler,
        // it is likely from [ProxyValidator] and we don't want to override it.
        if (pipeline.get("proxy") == null) {
            pipeline.addFirst("proxy", currentProxy?.handler() ?: return@handler)
        }
    }

}
