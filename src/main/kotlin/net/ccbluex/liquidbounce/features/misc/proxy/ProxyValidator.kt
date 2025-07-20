package net.ccbluex.liquidbounce.features.misc.proxy

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.clientChannelAndGroup
import net.minecraft.client.network.Address
import net.minecraft.client.network.AllowedAddressResolver
import net.minecraft.client.network.ServerAddress
import net.minecraft.network.ClientConnection
import net.minecraft.network.DisconnectionInfo
import net.minecraft.network.NetworkSide
import net.minecraft.network.listener.ClientQueryPacketListener
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket
import net.minecraft.server.ServerMetadata
import net.minecraft.util.Util
import java.net.InetSocketAddress
import kotlin.jvm.optionals.getOrNull


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
 *
 */

/**
 * This is a generic Minecraft server that is used to check if a proxy is working. The server also
 * responds to query requests with the client's IP address.
 */
private const val PING_SERVER = "ping.liquidproxy.net"
private const val PING_TIMEOUT = 5

class ClientConnectionTicker(private val clientConnection: ClientConnection) : EventListener {
    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        clientConnection.tick()
    }
}

/**
 * Checks if a proxy is valid and can be used for Minecraft. This will use network resources to check the proxy,
 * as well as update the ip information of the proxy.
 */
fun Proxy.check(success: (Proxy) -> Unit, failure: (Throwable) -> Unit) = runCatching {
    logger.info("Request ping server via proxy... [$host:$port]")

    val serverAddress = ServerAddress.parse(PING_SERVER)
    val socketAddress: InetSocketAddress = AllowedAddressResolver.DEFAULT.resolve(serverAddress)
        .map(Address::getInetSocketAddress)
        .getOrNull()
        ?: error("Failed to resolve $PING_SERVER")
    logger.info("Resolved ping server [$PING_SERVER]: $socketAddress")

    val clientConnection = ClientConnection(NetworkSide.CLIENTBOUND)
    val channelFuture = connect(socketAddress, false, clientConnection)
    channelFuture.syncUninterruptibly()

    val ticker = ClientConnectionTicker(clientConnection)

    val clientQueryPacketListener = object : ClientQueryPacketListener {

        private var serverMetadata: ServerMetadata? = null
        private var startTime = 0L

        override fun onResponse(packet: QueryResponseS2CPacket) {
            if (serverMetadata != null) {
                failure(IllegalStateException("Received multiple responses from server"))
                return
            }

            val metadata = packet.metadata()
            serverMetadata = metadata
            startTime = Util.getMeasuringTimeMs()
            clientConnection.send(QueryPingC2SPacket(startTime))
            logger.info("Proxy Metadata [$host:$port]: ${metadata.description.convertToString()}")
        }

        override fun onPingResult(packet: PingResultS2CPacket) {
            val serverMetadata = this.serverMetadata ?: error("Received ping result without metadata")
            val ping = Util.getMeasuringTimeMs() - startTime
            logger.info("Proxy Ping [$host:$port]: $ping ms")

            runCatching {
                val ipInfo = runBlocking(Dispatchers.IO) {
                    IpInfoApi.someoneElse(serverMetadata.description.convertToString())
                }
                this@check.ipInfo = ipInfo
                logger.info("Proxy Info [$host:$port]: ${ipInfo.ip} [${ipInfo.country}, ${ipInfo.org}]")
            }.onFailure { throwable ->
                logger.error("Failed to update IP info for proxy [$host:$port]", throwable)
            }

            success(this@check)
        }

        override fun onDisconnected(info: DisconnectionInfo) {
            EventManager.unregisterEventHandler(ticker)

            if (this.serverMetadata == null) {
                failure(IllegalStateException("Disconnected before receiving metadata"))
            }
        }

        override fun isConnectionOpen() = clientConnection.isOpen
    }

    clientConnection.connect(serverAddress.address, serverAddress.port, clientQueryPacketListener)
    clientConnection.send(QueryRequestC2SPacket.INSTANCE)
    logger.info("Sent query request via proxy [$host:$port]")
}.onFailure { throwable -> failure(throwable) }

private fun Proxy.connect(
    address: InetSocketAddress,
    useEpoll: Boolean,
    connection: ClientConnection
): ChannelFuture {
    return Bootstrap().clientChannelAndGroup(useEpoll).handler(object : ChannelInitializer<Channel>() {
        override fun initChannel(channel: Channel) {
            try {
                channel.config().setOption(ChannelOption.TCP_NODELAY, true)
            } catch (_: ChannelException) {}

            val channelPipeline = channel.pipeline().addLast("timeout", ReadTimeoutHandler(PING_TIMEOUT))
            // Assign proxy before [ClientConnection.addHandlers] to avoid overriding the proxy
            channelPipeline.addFirst("proxy", handler())
            ClientConnection.addHandlers(channelPipeline, NetworkSide.CLIENTBOUND, false, null)
            connection.addFlowControlHandler(channelPipeline)
        }
    }).connect(address.address, address.port)
}
