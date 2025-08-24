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

import com.google.gson.JsonElement
import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.gson.util.decode
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.mcef.utils.FileUtils as McefFileUtils
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Util
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import okio.sink
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.TimeUnit

val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun withScope(block: suspend CoroutineScope.() -> Unit) = scope.launch { block() }

object HttpClient {

    val DEFAULT_AGENT = "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersion}" +
        " (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, " +
        "${if (LiquidBounce.IN_DEVELOPMENT) "dev" else "release"}, ${System.getProperty("os.name")})"

    val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()

    /**
     * Client default [OkHttpClient]
     */
    @get:JvmStatic
    val client: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(Util.getDownloadWorkerExecutor().service))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request()
            try {
                val response = chain.proceed(request)

                if (response.isSuccessful) {
                    response
                } else {
                    // Response is not successful (code is not 2xx)
                    throw HttpException(enumValueOf(request.method),
                                        request.url.toString(), response.code, response.body.string())
                }
            } catch (e: IOException) {
                // Failed to request
                logger.error("Failed to execute request ${request.method} ${request.url})", e)
                throw e
            }
        }
        .build().also(McefFileUtils::setOkHttpClient)

    suspend fun request(
        url: String,
        method: HttpMethod,
        agent: String = DEFAULT_AGENT,
        headers: Headers.Builder.() -> Unit = {},
        body: RequestBody? = null
    ): Response = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .method(method.name, body)
            .headers(Headers.Builder().apply(headers).build())
            .header("User-Agent", agent)
            .build()

        client.newCall(request).execute()
    }

    suspend fun download(url: String, file: File, agent: String = DEFAULT_AGENT) =
        request(url, HttpMethod.GET, agent).toFile(file)

}

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD
}

/**
 * Parse body from [Response].
 *
 * If [T] is one of following types, it should be closed after using:
 * [InputStream] / [BufferedSource] / [Reader]
 */
inline fun <reified T> Response.parse(): T {
    return when (T::class.java) {
        String::class.java -> body.string() as T
        Unit::class.java -> close() as T
        InputStream::class.java -> body.byteStream() as T
        BufferedSource::class.java -> body.source() as T
        Reader::class.java -> body.charStream() as T
        NativeImageBackedTexture::class.java -> body.byteStream().use { stream ->
            NativeImageBackedTexture(NativeImage.read(stream))
        } as T
        else -> decode<T>(body.charStream())
    }
}

/**
 * Read all UTF-8 lines from [BufferedSource] as an [Iterator].
 *
 * When there are no more lines to read, the source is closed automatically.
 */
fun BufferedSource.utf8Lines(): Iterator<String> =
    object : AbstractIterator<String>() {
        override fun computeNext() {
            val nextLine = readUtf8Line()
            if (nextLine != null) {
                setNext(nextLine)
            } else {
                close()
                done()
            }
        }
    }

/**
 * Save response body to file.
 */
fun Response.toFile(file: File) = use { response ->
    file.sink().use(response.body.source()::readAll)
}

fun JsonElement.toRequestBody(): RequestBody {
    return accessibleInteropGson.toJson(this)
        .toRequestBody(HttpClient.JSON_MEDIA_TYPE)
}

fun String.asJson() = toRequestBody(HttpClient.JSON_MEDIA_TYPE)
fun String.asForm() = toRequestBody(HttpClient.FORM_MEDIA_TYPE)

class HttpException(val method: HttpMethod, val url: String, val code: Int, val content: String)
    : Exception("${method.name} $url failed with code $code: $content")
