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
 *
 */
package net.ccbluex.liquidbounce.web.socket.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import java.net.Socket


internal class NettyServer {

    companion object {

        private const val DEFAULT_PORT = 15000

        val PORT = findAvailablePort()
        val NETTY_ROOT = "http://127.0.0.1:$PORT"

        @Suppress("SwallowedException")
        private fun findAvailablePort() = try {
            Socket("localhost", DEFAULT_PORT).use {
                logger.info("Default port unavailable. Falling back to random port.")
                (15001..17000).random()
            }
        } catch (e: Exception) {
            logger.info("Default port $DEFAULT_PORT available.")
            DEFAULT_PORT
        }
    }

    fun startServer(port: Int = PORT) {
        val bossGroup = if (Epoll.isAvailable()) EpollEventLoopGroup() else NioEventLoopGroup()
        val workerGroup = if (Epoll.isAvailable()) EpollEventLoopGroup() else NioEventLoopGroup()

        try {
            logger.info("Starting Netty server...")
            val b = ServerBootstrap()
            b.option(ChannelOption.SO_BACKLOG, 1024)
            b.group(bossGroup, workerGroup)
                .channel(if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(HttpChannelInitializer())
            val ch = b.bind(port).sync().channel()

            logger.info("Netty server started on port $port.")
            ch.closeFuture().sync()
        } catch (e: InterruptedException) {
            logger.error("Netty server interrupted", e)
        } catch (t: Throwable) {
            logger.error("Netty server failed - $port", t)
            ErrorHandler.fatal(t, "Port: $port")
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

        logger.info("Netty server stopped.")
    }

}
