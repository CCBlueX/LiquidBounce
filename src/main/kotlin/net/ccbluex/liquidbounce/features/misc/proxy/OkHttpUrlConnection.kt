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

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A GET-only [HttpURLConnection] backed by OkHttp, for vanilla code that only takes a connection.
 *
 * Over SOCKS, the JDK resolves the target host locally, and it refuses Basic auth for HTTPS tunnels. OkHttp does
 * neither.
 */
internal class OkHttpUrlConnection(url: URL, private val client: OkHttpClient) : HttpURLConnection(url) {

    private var response: Response? = null

    override fun connect() {
        if (response != null) {
            return
        }

        val headers = Headers.Builder()
        requestProperties.forEach { (name, values) -> values.forEach { headers.add(name, it) } }
        response = client.newCall(Request.Builder().url(url).headers(headers.build()).build()).execute()
        connected = true
    }

    private fun response(): Response {
        connect()
        return response!!
    }

    override fun getInputStream(): InputStream = response().body.byteStream()

    override fun getContentLengthLong() = response().body.contentLength()

    override fun getResponseCode() = response().code

    override fun getHeaderField(name: String): String? = response().header(name)

    override fun disconnect() {
        response?.close()
    }

    override fun usingProxy() = true

}
