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

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelException
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.clientChannelAndGroup
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.client.multiplayer.resolver.ServerNameResolver
import net.minecraft.network.Connection
import net.minecraft.network.DisconnectionDetails
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket
import net.minecraft.network.protocol.status.ClientStatusPacketListener
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket
import net.minecraft.network.protocol.status.ServerStatus
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket
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

/**
 * Checks if a proxy is valid and can be used for Minecraft. This will use network resources to check the proxy,
 * as well as update the ip information of the proxy.
 */
fun Proxy.check(success: (Proxy) -> Unit, failure: (Throwable) -> Unit) = runCatching {
    logger.info("Request ping server via proxy... [$host:$port]")

    val serverAddress = ServerAddress.parseString(PING_SERVER)
    val socketAddress: InetSocketAddress = ServerNameResolver.DEFAULT.resolveAddress(serverAddress)
        .map(ResolvedServerAddress::asInetSocketAddress)
        .getOrNull()
        ?: error("Failed to resolve $PING_SERVER")
    logger.info("Resolved ping server [$PING_SERVER]: $socketAddress")

    val clientConnection = Connection(PacketFlow.CLIENTBOUND)
    val channelFuture = connect(socketAddress, true, clientConnection)
        .syncUninterruptibly()

    // Channel is ready after connection future
    val scope = CoroutineScope(channelFuture.channel().eventLoop().asCoroutineDispatcher())

    // Add to tick list
    ProxyManager.addClientConnection(clientConnection)

    val clientQueryPacketListener = object : ClientStatusPacketListener {

        private var serverMetadata: ServerStatus? = null
        private var startTime = 0L

        override fun handleStatusResponse(packet: ClientboundStatusResponsePacket) {
            if (serverMetadata != null) {
                failure(IllegalStateException("Received multiple responses from server"))
                return
            }

            val metadata = packet.status()
            serverMetadata = metadata
            startTime = Util.getMillis()
            clientConnection.send(ServerboundPingRequestPacket(startTime))
            logger.info("Proxy Metadata [$host:$port]: ${metadata.description.string}")
        }

        override fun handlePongResponse(packet: ClientboundPongResponsePacket) {
            scope.launch {
                val serverMetadata = serverMetadata ?: error("Received ping result without metadata")
                val ping = Util.getMillis() - startTime
                logger.info("Proxy Ping [$host:$port]: $ping ms")

                runCatching {
                    val ipInfo = IpInfoApi.someoneElse(serverMetadata.description.string)
                    this@check.ipInfo = ipInfo
                    logger.info("Proxy Info [$host:$port]: ${ipInfo.ip} [${ipInfo.country}, ${ipInfo.org}]")
                }.onFailure { throwable ->
                    logger.error("Failed to update IP info for proxy [$host:$port]", throwable)
                }

                success(this@check)
            }
        }

        override fun onDisconnect(info: DisconnectionDetails) {
            if (this.serverMetadata == null) {
                failure(IllegalStateException("Disconnected before receiving metadata"))
            }
        }

        override fun isAcceptingMessages() = clientConnection.isConnected
    }

    clientConnection.initiateServerboundStatusConnection(
        serverAddress.host,
        serverAddress.port,
        clientQueryPacketListener
    )
    clientConnection.send(ServerboundStatusRequestPacket.INSTANCE)
    logger.info("Sent query request via proxy [$host:$port]")
}.onFailure { throwable -> failure(throwable) }

private fun Proxy.connect(
    address: InetSocketAddress,
    useEpoll: Boolean,
    connection: Connection
): ChannelFuture {
    return Bootstrap().clientChannelAndGroup(useEpoll).handler(object : ChannelInitializer<Channel>() {
        override fun initChannel(channel: Channel) {
            try {
                channel.config().setOption(ChannelOption.TCP_NODELAY, true)
            } catch (_: ChannelException) {}

            val channelPipeline = channel.pipeline().addLast("timeout", ReadTimeoutHandler(PING_TIMEOUT))
            // Assign proxy before [ClientConnection.addHandlers] to avoid overriding the proxy
            channelPipeline.addFirst("proxy", handler())
            Connection.configureSerialization(channelPipeline, PacketFlow.CLIENTBOUND, false, null)
            connection.configurePacketHandler(channelPipeline)
        }
    }).connect(address.address, address.port)
}
