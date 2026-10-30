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
package net.ccbluex.liquidbounce.integration.task

import kotlinx.coroutines.CompletableDeferred
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.SocketAddress

/** Proxy selection for the browser and deep learning dependency downloads. */
object DownloadProxy {
    sealed interface Choice {
        data object Direct : Choice
        data object System : Choice
        data class Custom(val address: InetSocketAddress) : Choice
    }

    @Volatile
    var choice: Choice = Choice.Direct

    @Volatile
    var onlineInitialization = false

    @Volatile
    var pending: CompletableDeferred<Choice>? = null

    private val downloadClient = ThreadLocal.withInitial { false }
    val wantsDownloadClient: Boolean get() = downloadClient.get()

    fun initializationSelector(delegate: ProxySelector): ProxySelector = object : ProxySelector() {
        override fun select(uri: URI): List<Proxy> =
            if (onlineInitialization) selector(delegate).select(uri) else listOf(Proxy.NO_PROXY)

        override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {
            if (onlineInitialization) selector(delegate).connectFailed(uri, address, error)
        }
    }

    fun <T> withDownloadClient(block: () -> T): T {
        val previous = downloadClient.get()
        downloadClient.set(true)
        return try { block() } finally { downloadClient.set(previous) }
    }

    fun selector(delegate: ProxySelector, selected: Choice = choice): ProxySelector = object : ProxySelector() {
        override fun select(uri: URI): List<Proxy> {
            val host = uri.host ?: return listOf(Proxy.NO_PROXY)
            if (host.equals("localhost", true) || host.startsWith("127.") || host == "::1" || host == "[::1]") {
                return listOf(Proxy.NO_PROXY)
            }
            return when (selected) {
                Choice.Direct -> listOf(Proxy.NO_PROXY)
                Choice.System -> delegate.select(uri)
                is Choice.Custom -> listOf(Proxy(Proxy.Type.HTTP, selected.address))
            }
        }

        override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {
            if (selected == Choice.System) delegate.connectFailed(uri, address, error)
        }
    }

    /** DJL uses URLConnection. Route only the thread executing its synchronous initialization. */
    fun <T> withJavaDownloadProxy(block: () -> T): T {
        val previous = ProxySelector.getDefault()
        val downloadThread = Thread.currentThread()
        val downloadSelector = selector(previous)
        val scoped = object : ProxySelector() {
            override fun select(uri: URI): List<Proxy> =
                if (Thread.currentThread() === downloadThread) downloadSelector.select(uri) else previous.select(uri)

            override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {
                if (Thread.currentThread() === downloadThread) {
                    downloadSelector.connectFailed(uri, address, error)
                } else {
                    previous.connectFailed(uri, address, error)
                }
            }
        }
        ProxySelector.setDefault(scoped)
        return try { block() } finally {
            if (ProxySelector.getDefault() === scoped) ProxySelector.setDefault(previous)
        }
    }

    fun parseAddress(value: String): InetSocketAddress {
        val uri = URI("http://${value.trim()}")
        require(uri.host != null && uri.port in 1..65535 && uri.userInfo == null &&
            uri.rawPath.isNullOrEmpty() && uri.rawQuery == null && uri.rawFragment == null) {
            "Enter an HTTP proxy as host:port."
        }
        return InetSocketAddress.createUnresolved(uri.host, uri.port)
    }
}
