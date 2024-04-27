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
package net.ccbluex.liquidbounce.api

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import java.net.URI

object IpInfoApi {

    private const val API_URL = "https://ipinfo.io/json"

    var localIpInfo: IpInfo? = null
        private set

    /**
     * Refresh local IP info
     */
    fun refreshLocalIpInfo() {
        requestIpInfo(success = { ipInfo ->
            logger.info("IP Info [${ipInfo.country}, ${ipInfo.org}]")
            this.localIpInfo = ipInfo
        }, failure = {
            logger.error("Failed to refresh local IP info", it)
        })
    }

    /**
     * Request IP info from API
     */
    fun requestIpInfo(proxy: ProxyManager.Proxy? = ProxyManager.currentProxy,
                      success: (IpInfo) -> Unit,
                      failure: (Throwable) -> Unit
    ) = makeAsyncEndpointRequest(proxy = proxy, endpoint = API_URL, success = {
        success(decode<IpInfo>(it))
    }, failure = failure)

    /**
     * Request to endpoint async and with proxy
     */
    private fun makeAsyncEndpointRequest(proxy: ProxyManager.Proxy?, endpoint: String,
                                         success: (String) -> Unit, failure: (Throwable) -> Unit) = runCatching {
        val uri = URI(endpoint)
        val group = NioEventLoopGroup()

        val ssl = uri.scheme == "https"
        val sslContext = if (ssl) {
            SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
        } else {
            null
        }

        try {
            val bootstrap = Bootstrap()
                .group(group)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 5 seconds timeout
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(p0: SocketChannel) {
                        val pipeline = p0.pipeline()

                        ProxyManager.insertProxyHandler(proxy, pipeline)
                        if (sslContext != null) {
                            pipeline.addLast(sslContext.newHandler(p0.alloc()))
                        }
                        pipeline.addLast(HttpClientCodec())
                        pipeline.addLast(HttpContentDecompressor())
                        pipeline.addLast(object : SimpleChannelInboundHandler<HttpObject>() {

                            val buffer = StringBuilder()

                            override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
                                if (msg is HttpResponse) {
                                    // Throw error if status code is not 200
                                    if (msg.status().code() != 200) {
                                        error("Invalid status code: ${msg.status().code()}")
                                    }
                                }

                                if (msg is HttpContent) {
                                    val content = msg.content().toString(Charsets.UTF_8)
                                    buffer.append(content)

                                    if (msg is LastHttpContent) {
                                        success(buffer.toString())
                                        ctx.close()
                                    }
                                }

                                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER)
                                    .addListener(ChannelFutureListener.CLOSE)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                failure(cause)
                                ctx.close()
                            }

                        })
                    }
                })

            val port = if (uri.port == -1) {
                if (ssl) 443 else 80
            } else {
                uri.port
            }
            val channel = bootstrap.connect(uri.host, port).sync().channel()

            val httpRequest = DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                uri.rawPath
            )

            httpRequest.headers().set(HttpHeaderNames.HOST, uri.host)
            httpRequest.headers().set(HttpHeaderNames.USER_AGENT, "LiquidBounce ${LiquidBounce.clientVersion}")
            httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaders.Values.CLOSE)
            httpRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaders.Values.GZIP)

            channel.writeAndFlush(httpRequest).sync()
            channel.closeFuture().sync()
        } catch(it: Throwable) {
            failure(it)
        } finally {
            group.shutdownGracefully()
        }
    }.onFailure(failure)
}

data class IpInfo(
    val ip: String?,
    val hostname: String?,
    val city: String?,
    val region: String?,
    val country: String?,
    val loc: String?,
    val org: String?,
    val postal: String?,
    val timezone: String?
)
