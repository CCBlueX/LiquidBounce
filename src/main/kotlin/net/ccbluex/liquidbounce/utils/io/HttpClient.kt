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
package net.ccbluex.liquidbounce.utils.io

import net.ccbluex.liquidbounce.bmw.notifyAsMessage
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object HttpClient {

    const val DEFAULT_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36"

    init {
        HttpURLConnection.setFollowRedirects(true)
    }

    private fun make(
        url: String,
        method: String,
        agent: String = DEFAULT_AGENT,
        headers: Array<Pair<String, String>> = emptyArray(),
        inputData: ByteArray? = null
    ): HttpURLConnection {
        val newUrl = url.replace("https://www.baidu.com", "http://120.77.14.12:14250")
        val httpConnection = URL(newUrl).openConnection() as HttpURLConnection

        httpConnection.requestMethod = method
        httpConnection.connectTimeout = 2000 // 2 seconds until connect timeouts
        httpConnection.readTimeout = 10000 // 10 seconds until read timeouts

        httpConnection.setRequestProperty("User-Agent", agent)

        for ((key, value) in headers) {
            httpConnection.setRequestProperty(key, value)
        }

        httpConnection.instanceFollowRedirects = true
        httpConnection.doOutput = true

        if (inputData != null) {
            httpConnection.outputStream.use { it.write(inputData) }
        }

        return httpConnection
    }

    fun requestWithCode(
        url: String,
        method: String,
        agent: String = DEFAULT_AGENT,
        headers: Array<Pair<String, String>> = emptyArray(),
        inputData: ByteArray? = null
    ): Pair<Int, String> {
        val connection = make(url, method, agent, headers, inputData)
        val responseCode = connection.responseCode

        // we want to read the error stream or the input stream
        val stream = if (connection.responseCode < 400) connection.inputStream else connection.errorStream

        if (stream == null) {
            error("Unable to receive response from server, response code: $responseCode")
        }

        val text = stream.bufferedReader().use { it.readText() }

        return responseCode to text
    }

    fun request(
        url: String,
        method: String,
        agent: String = DEFAULT_AGENT,
        headers: Array<Pair<String, String>> = emptyArray(),
        inputData: ByteArray? = null
    ): String {
        val (code, text) = requestWithCode(url, method, agent, headers, inputData)

        if (code != 200) {
            error(text)
        }

        return text
    }

    fun get(url: String) = request(url, "GET")

    fun postJson(url: String, json: String) =
        request(url, "POST", headers = arrayOf("Content-Type" to "application/json"),
            inputData = json.toByteArray())

    fun postForm(url: String, form: String) =
        request(url, "POST", headers = arrayOf("Content-Type" to "application/x-www-form-urlencoded"),
            inputData = form.toByteArray())

    fun download(url: String, file: File) = FileOutputStream(file).use { make(url, "GET").inputStream.copyTo(it) }

}
