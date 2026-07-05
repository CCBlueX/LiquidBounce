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
package net.ccbluex.liquidbounce.utils.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFactory
import io.netty.channel.EventLoopGroup
import io.netty.channel.IoHandlerFactory
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import java.util.concurrent.ThreadFactory

// ---- Netty Transport Setup ----

private enum class TransportType(
    val serverChannelFactory: ChannelFactory<out ServerChannel>,
) {
    NIO(::NioServerSocketChannel) {
        override val isAvailable get() = true
        override val ioHandlerFactory: IoHandlerFactory = NioIoHandler.newFactory()
    },

    EPOLL(::EpollServerSocketChannel) {
        override val isAvailable get() = try {
            Epoll.isAvailable()
        } catch (_: Throwable) { false }
        override val ioHandlerFactory: IoHandlerFactory get() = EpollIoHandler.newFactory()
    },

    KQUEUE(::KQueueServerSocketChannel) {
        override val isAvailable get() = try {
            KQueue.isAvailable()
        } catch (_: Throwable) { false }
        override val ioHandlerFactory: IoHandlerFactory get() = KQueueIoHandler.newFactory()
    };

    abstract val isAvailable: Boolean

    abstract val ioHandlerFactory: IoHandlerFactory
}

private val availableTransport by lazy {
    arrayOf(TransportType.EPOLL, TransportType.KQUEUE, TransportType.NIO)
        .first { it.isAvailable }
}

@JvmOverloads
fun ServerBootstrap.setup(
    useNativeTransport: Boolean = true,
    threadFactory: ThreadFactory? = null,
): Pair<EventLoopGroup, EventLoopGroup> {
    val type = if (useNativeTransport) availableTransport else TransportType.NIO

    val parentGroup = MultiThreadIoEventLoopGroup(1, threadFactory, type.ioHandlerFactory)
    val childGroup = MultiThreadIoEventLoopGroup(threadFactory, type.ioHandlerFactory)
    group(parentGroup, childGroup)
        .channelFactory(type.serverChannelFactory)
    return parentGroup to childGroup
}

// ---- Netty Future Coroutine Bridges ----

/**
 * Suspend until this Netty Future completes,
 * and rethrows the cause of the failure if this future failed.
 */
suspend fun <V, F : Future<V>> F.syncSuspend(): F {
    if (isDone) return unwrapDone().getOrThrow()

    return suspendCancellableCoroutine { cont ->
        addListener(FutureResultContListener(cont))

        cont.invokeOnCancellation {
            this.cancel(false)
        }
    }
}

/**
 * Suspend until this Netty Future completes.
 */
suspend fun <F : Future<*>> F.awaitSuspend(): F {
    if (isDone) return this

    return suspendCancellableCoroutine { cont ->
        addListener(FutureContListener(cont))

        cont.invokeOnCancellation {
            this.cancel(false)
        }
    }
}

private class FutureContListener<V, F : Future<V>>(
    private val cont: CancellableContinuation<F>
) : GenericFutureListener<F> {
    override fun operationComplete(future: F) {
        if (cont.isActive) {
            cont.resumeWith(Result.success(future))
        }
    }
}

private class FutureResultContListener<V, F : Future<V>>(
    private val cont: CancellableContinuation<F>
) : GenericFutureListener<F> {
    override fun operationComplete(future: F) {
        if (cont.isActive) {
            cont.resumeWith(future.unwrapDone())
        }
    }
}

private fun <V, F : Future<V>> F.unwrapDone(): Result<F> =
    when {
        isSuccess -> Result.success(this)
        isCancelled -> Result.failure(CancellationException("Netty Future was cancelled"))
        else -> Result.failure(
            this.cause() ?: IllegalStateException("Future failed without cause")
        )
    }
