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
import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PipelineEvent
import net.ccbluex.liquidbounce.event.events.ProxyCheckResultEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.network.ClientConnection

/**
 * Proxy Manager
 *
 * Only supports SOCKS5 proxies.
 */
object ProxyManager : Configurable("proxy"), EventListener {

    var proxy by value("selectedProxy", Proxy.NONE, valueType = ValueType.PROXY).onChanged {
        ConfigSystem.store(this)
    }
    internal val proxies by list(name, mutableListOf<Proxy>(), valueType = ValueType.PROXY)

    /**
     * The proxy that is set in the current session and used for all server connections
     */
    val currentProxy
        get() = proxy.takeIf { proxy -> proxy.host.isNotBlank() && proxy.port > 0 }

    private val clientConnections = mutableListOf<ClientConnection>()

    internal fun addClientConnection(connection: ClientConnection) = mc.execute {
        clientConnections.add(connection)
    }

    init {
        ConfigSystem.root(this)
    }

    /**
     * Validate if [proxy] is working by sending a query-request to our Minecraft Ping Server.
     * This eliminates proxy that only work for HTTP(S) connections.
     *
     * This function also automatically adds the proxy to our list, if [index] is not provided.
     * If [index] is provided, it will swap out the proxy at that index with the new one.
     * If [checkOnly] is true, we will skip adding the proxy to our list.
     */
    fun validateProxy(proxy: Proxy, index: Int? = null, checkOnly: Boolean = false) = proxy.check(
        success = { proxy ->
            logger.info("Successfully checked proxy [${proxy.host}:${proxy.port}]")
            EventManager.callEvent(ProxyCheckResultEvent(proxy = proxy))

            if (checkOnly) {
                return@check
            }

            val isConnected = index != null && this@ProxyManager.proxy == proxies[index]
            if (index != null) {
                proxies[index] = proxy
            } else {
                // If no index is provided, we are adding a new proxy
                proxies.add(proxy)
            }
            ConfigSystem.store(this)

            if (isConnected) {
                this@ProxyManager.proxy = proxy
            }
        },
        failure = {
            logger.error("Failed to check proxy", it)
            EventManager.callEvent(ProxyCheckResultEvent(proxy, error = it.message ?: "Unknown error"))
        }
    )

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

    /**
     * Handles proxy validation connections.
     */
    @Suppress("unused")
    private val connectionTicker = handler<GameTickEvent> {
        clientConnections.removeIf {
            if (it.isOpen) {
                it.tick()
                true
            } else {
                false
            }
        }
    }

}
