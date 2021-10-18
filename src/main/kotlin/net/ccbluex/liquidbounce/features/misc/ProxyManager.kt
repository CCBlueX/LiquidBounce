/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import io.netty.channel.*
import io.netty.handler.proxy.Socks5ProxyHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import net.minecraft.network.*
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

    fun setupConnect(clientConnection: ClientConnection) = object : ChannelInitializer<Channel>() {
        @Throws(Exception::class)
        override fun initChannel(channel: Channel) {
            try {
                channel.config().setOption(ChannelOption.TCP_NODELAY, true)
            } catch (var3: ChannelException) {
            }

            val p = channel.pipeline()
                .addLast("timeout", ReadTimeoutHandler(30) as ChannelHandler)
                .addLast("splitter", SplitterHandler() as ChannelHandler)
                .addLast("decoder", DecoderHandler(NetworkSide.CLIENTBOUND) as ChannelHandler)
                .addLast("prepender", SizePrepender() as ChannelHandler)
                .addLast("encoder", PacketEncoder(NetworkSide.SERVERBOUND) as ChannelHandler)
                .addLast("packet_handler", clientConnection as ChannelHandler?)

            val proxy = currentProxy
            if (proxy != null) {
                p.addFirst(
                    "proxy",
                    if (proxy.credentials != null) {
                        Socks5ProxyHandler(proxy.address, proxy.credentials.username, proxy.credentials.password)
                    } else {
                        Socks5ProxyHandler(proxy.address)
                    }
                )
            }
        }
    }

    data class Proxy(val address: InetSocketAddress, val credentials: ProxyCredentials?)

    data class ProxyCredentials(val username: String, val password: String)

}
