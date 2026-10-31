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

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists

/** Queries the JVM's system proxy selector without changing the running game's global proxy configuration. */
internal class SystemProxySelector : ProxySelector() {
    private val cache = ConcurrentHashMap<URI, List<Proxy>>()

    override fun select(uri: URI): List<Proxy> {
        val host = requireNotNull(uri.host) { "Proxy query requires a host" }
        // Do not pass URLs, paths, query strings or credentials to the helper process.
        val authority = URI(uri.scheme, null, host, uri.port, null, null, null)
        return cache.computeIfAbsent(authority, ::query)
    }

    override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) = Unit

    private fun query(uri: URI): List<Proxy> {
        val javaHome = Path.of(System.getProperty("java.home"), "bin")
        val windowsExecutable = javaHome.resolve("java.exe")
        val executable = if (windowsExecutable.exists()) windowsExecutable else javaHome.resolve("java")
        val source = Path.of(SystemProxyQuery::class.java.protectionDomain.codeSource.location.toURI())
        val process = ProcessBuilder(
            executable.toString(), "-Djava.net.useSystemProxies=true", "-cp", source.toString(),
            SystemProxyQuery::class.java.name, uri.toString()
        ).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("System proxy lookup timed out")
        }
        check(process.exitValue() == 0) { "System proxy lookup failed" }
        val lines = process.inputStream.bufferedReader().use { it.readLines() }
        check(lines.isNotEmpty()) { "System proxy lookup returned no result" }
        return lines.map { line ->
            if (line == "DIRECT") {
                Proxy.NO_PROXY
            } else {
                val parts = line.split('\t')
                check(parts.size == 3) { "Invalid system proxy response" }
                val type = Proxy.Type.valueOf(parts[0])
                val host = String(Base64.getDecoder().decode(parts[1]), Charsets.UTF_8)
                Proxy(type, InetSocketAddress.createUnresolved(host, parts[2].toInt()))
            }
        }
    }
}
