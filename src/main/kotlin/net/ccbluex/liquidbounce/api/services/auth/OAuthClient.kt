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
package net.ccbluex.liquidbounce.api.services.auth

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_AUTHORIZE_URL
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_CLIENT_ID
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import java.net.InetSocketAddress
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * OAuth client for handling the authentication flow
 */
object OAuthClient {

    private var serverPort: Int? = null
    @Volatile
    private var authCodeContinuation: Continuation<String>? = null

    /**
     * Start the OAuth authentication flow
     *
     * @param onUrl Callback for when the authorization URL is ready
     * @return Client account with the authenticated session
     */
    suspend fun startAuth(onUrl: (String) -> Unit): ClientAccount {
        val (codeVerifier, codeChallenge) = PKCEUtils.generatePKCE()
        val state = UUID.randomUUID().toString()

        if (serverPort == null) {
            serverPort = startNettyServer()
        }

        val redirectUri = "http://127.0.0.1:$serverPort/"
        val authUrl = buildAuthUrl(codeChallenge, state, redirectUri)

        onUrl(authUrl)
        val code = waitForAuthCode()
        val tokenResponse = AuthenticationApi.exchangeToken(AUTH_CLIENT_ID, code, codeVerifier, redirectUri)

        serverPort = null

        return ClientAccount(session = tokenResponse.toAuthSession())
    }

    /**
     * Renew an expired session using its refresh token
     */
    suspend fun renewToken(session: OAuthSession): OAuthSession {
        val tokenResponse = AuthenticationApi.refreshToken(AUTH_CLIENT_ID, session.refreshToken)
        return tokenResponse.toAuthSession()
    }

    private suspend fun startNettyServer(): Int = suspendCoroutine { cont ->
        withScope {
            runCatching {
                val bossGroup = NioEventLoopGroup(1)
                val workerGroup = NioEventLoopGroup()

                try {
                    val bootstrap = ServerBootstrap()
                    bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel::class.java)
                        .childHandler(NettyChannelInitializer())

                    val channelFuture: ChannelFuture = bootstrap.bind(0).sync()
                    val localPort = (channelFuture.channel().localAddress() as InetSocketAddress).port
                    cont.resume(localPort)

                    // Keep server running until closed
                    channelFuture.channel().closeFuture().sync()
                } finally {
                    bossGroup.shutdownGracefully()
                    workerGroup.shutdownGracefully()
                }
            }.onFailure { e -> cont.resumeWithException(e) }
        }
    }

    class NettyChannelInitializer : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast(HttpServerCodec(), HttpObjectAggregator(65536), NettyAuthHandler())
        }
    }

    class NettyAuthHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
            val uri = msg.uri()
            val uriData = QueryStringDecoder(uri)
            val queryParameters = uriData.parameters().mapValues { it.value[0] }
            val code = queryParameters["code"]

            authCodeContinuation?.let {
                if (code != null) {
                    val response = DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK
                    ).apply {
                        content().writeBytes(SUCCESS_HTML.toByteArray())
                        headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8")
                        headers().set(HttpHeaderNames.CONTENT_LENGTH, content().readableBytes())
                    }
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
                    it.resume(code)
                } else {
                    it.resumeWithException(Exception("No code found in the redirect URL"))
                }
                authCodeContinuation = null
            }
        }
    }

    private inline fun buildAuthUrl(codeChallenge: String, state: String, redirectUri: String): String {
        return "$AUTH_AUTHORIZE_URL?client_id=$AUTH_CLIENT_ID&redirect_uri=$redirectUri&" +
            "response_type=code&state=$state&code_challenge=$codeChallenge&code_challenge_method=S256"
    }

    private suspend fun waitForAuthCode(): String = suspendCoroutine { cont ->
        authCodeContinuation = cont
    }

    private const val SUCCESS_HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Authentication Successful</title>
            <style>
                body { font-family: Arial, sans-serif; background-color: #121212; color: #ffffff; text-align: center; padding: 50px; }
                .container { background-color: #1E1E1E; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.5); display: inline-block; }
                h1 { color: #4CAF50; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Authentication Successful</h1>
                <p>You have successfully authenticated. You can close this tab now.</p>
            </div>
        </body>
        </html>
    """
}
