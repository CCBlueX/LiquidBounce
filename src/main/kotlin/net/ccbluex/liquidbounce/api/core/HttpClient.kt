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
package net.ccbluex.liquidbounce.api.core

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.gson.util.decode
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun withScope(block: suspend CoroutineScope.() -> Unit) = scope.launch { block() }

object HttpClient {

    val DEFAULT_AGENT = "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersion}" +
        " (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, " +
        "${if (LiquidBounce.IN_DEVELOPMENT) "dev" else "release"}, ${System.getProperty("os.name")})"

    val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun createRequest(
        url: String,
        method: HttpMethod,
        agent: String = DEFAULT_AGENT,
        headers: Headers = Headers.Builder().build(),
        body: RequestBody? = null
    ) = Request.Builder()
        .url(url)
        .method(method.name, body)
        .header("User-Agent", agent)
        .headers(headers)
        .build()

    suspend fun request(
        url: String,
        method: HttpMethod,
        agent: String = DEFAULT_AGENT,
        headers: Headers.Builder.() -> Unit = {},
        body: RequestBody? = null
    ): Response = withContext(Dispatchers.IO) {
        val request = createRequest(url, method, agent, Headers.Builder().apply(headers).build(), body)

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw HttpException(method, url, response.code, response.body.string())
        }
        response
    }

    suspend fun download(url: String, file: File, agent: String = DEFAULT_AGENT) =
        request(url, HttpMethod.GET, agent).toFile(file)

}

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

inline fun <reified T> Response.parse(): T {
    return use {
        when (T::class.java) {
            String::class.java -> body.string() as T
            Unit::class.java -> Unit as T
            InputStream::class.java -> body.byteStream() as T
            BufferedSource::class.java -> body.source() as T
            NativeImageBackedTexture::class.java -> body.byteStream().use { stream ->
                NativeImageBackedTexture(NativeImage.read(stream)) as T
            }
            else -> decode<T>(body.charStream())
        }
    }
}

fun Response.toFile(file: File) = use { response ->
    file.sink().buffer().use(response.body.source()::readAll)
}

fun String.asJson() = toRequestBody(HttpClient.JSON_MEDIA_TYPE)
fun String.asForm() = toRequestBody(HttpClient.FORM_MEDIA_TYPE)

class HttpException(val method: HttpMethod, val url: String, val code: Int, message: String)
    : Exception("${method.name} $url failed with code $code: $message")
