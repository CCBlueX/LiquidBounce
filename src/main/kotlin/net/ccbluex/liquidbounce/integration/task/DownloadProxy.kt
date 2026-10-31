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
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.mc
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
        data class Custom(val address: InetSocketAddress, val type: Proxy.Type = Proxy.Type.HTTP) : Choice {
            init { require(type == Proxy.Type.HTTP || type == Proxy.Type.SOCKS) }
        }
    }

    @Volatile
    var choice: Choice = Choice.Direct

    @Volatile
    var onlineInitialization = false

    @Volatile
    var pending: CompletableDeferred<Choice>? = null

    private val downloadClient = ThreadLocal.withInitial { false }
    private val systemProxy = SystemProxySelector()
    val wantsDownloadClient: Boolean get() = downloadClient.get()

    fun openSettings() {
        val parent = mc.gui.screen()
        val path = ConfigSystem.rootFolder.toPath().resolve("startup-proxy.properties")
        val saved = DownloadProxyStorage.load(path) ?: Choice.Direct
        mc.gui.setScreen(DownloadProxyScreen(CompletableDeferred(), saved, parent) {
            DownloadProxyStorage.save(path, it)
        })
    }

    fun initializationSelector(): ProxySelector = object : ProxySelector() {
        override fun select(uri: URI): List<Proxy> =
            if (onlineInitialization) selector().select(uri) else listOf(Proxy.NO_PROXY)

        override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {
            if (onlineInitialization) selector().connectFailed(uri, address, error)
        }
    }

    fun <T> withDownloadClient(block: () -> T): T {
        val previous = downloadClient.get()
        downloadClient.set(true)
        return try { block() } finally { downloadClient.set(previous) }
    }

    fun selector(
        selected: Choice = choice,
        system: ProxySelector = systemProxy,
    ): ProxySelector = object : ProxySelector() {
        override fun select(uri: URI): List<Proxy> {
            val host = uri.host ?: return listOf(Proxy.NO_PROXY)
            if (host.equals("localhost", true) || host.startsWith("127.") || host == "::1" || host == "[::1]") {
                return listOf(Proxy.NO_PROXY)
            }
            return when (selected) {
                Choice.Direct -> listOf(Proxy.NO_PROXY)
                Choice.System -> system.select(uri)
                is Choice.Custom -> listOf(Proxy(selected.type, selected.address))
            }
        }

        override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {
            if (selected == Choice.System) system.connectFailed(uri, address, error)
        }
    }

    /** DJL uses URLConnection. Route only the thread executing its synchronous initialization. */
    fun <T> withJavaDownloadProxy(block: () -> T): T {
        val previous = ProxySelector.getDefault()
        val downloadThread = Thread.currentThread()
        val downloadSelector = selector()
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
            "Enter a proxy as host:port."
        }
        return InetSocketAddress.createUnresolved(uri.host, uri.port)
    }
}
