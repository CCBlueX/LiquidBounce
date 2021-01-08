/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.utils

import com.google.common.io.ByteStreams
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object HttpUtils {

    private const val DEFAULT_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36 Edg/87.0.664.60"

    init {
        HttpURLConnection.setFollowRedirects(true)
    }

    private fun make(url: String, method: String, agent: String = DEFAULT_AGENT): HttpURLConnection {
        val httpConnection = URL(url).openConnection() as HttpURLConnection

        httpConnection.requestMethod = method
        httpConnection.connectTimeout = 2000 // 2 seconds until connect timeouts
        httpConnection.readTimeout = 10000 // 10 seconds until read timeouts

        httpConnection.setRequestProperty("User-Agent", agent)

        httpConnection.instanceFollowRedirects = true
        httpConnection.doOutput = true

        return httpConnection
    }

    fun request(url: String, method: String, agent: String = DEFAULT_AGENT): String {
        val connection = make(url, method, agent)

        return connection.inputStream.reader().readText()
    }

    fun requestStream(url: String, method: String, agent: String = DEFAULT_AGENT): InputStream {
        val connection = make(url, method, agent)

        return connection.inputStream
    }

    fun get(url: String) = request(url, "GET")

    fun download(url: String, file: File) = FileOutputStream(file).use { ByteStreams.copy(make(url, "GET").inputStream, it) }

}
