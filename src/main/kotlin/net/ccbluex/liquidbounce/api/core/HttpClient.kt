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
package net.ccbluex.liquidbounce.api.core

import com.mojang.blaze3d.platform.NativeImage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.interceptors.CacheBlacklistInterceptor
import net.ccbluex.liquidbounce.authlib.Authlib
import net.ccbluex.liquidbounce.authlib.interceptor.DefaultHeaderInterceptor
import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.mcef.listeners.OkHttpProgressInterceptor
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import net.minecraft.ReportedException
import net.minecraft.util.Util
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import okio.BufferedSource
import okio.sink
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import net.ccbluex.liquidbounce.mcef.utils.FileUtils as McefFileUtils

val renderScope = CoroutineScope(
    Dispatchers.Minecraft + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        if (throwable is ReportedException) {
            ErrorHandler.fatal(throwable, additionalMessage = "Render scope")
        }
    }
)

val ioScope = CoroutineScope(
    Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        if (throwable is ReportedException) {
            ErrorHandler.fatal(throwable, additionalMessage = "IO scope")
        }
    }
)

fun withScope(block: suspend CoroutineScope.() -> Unit) = ioScope.launch { block() }

object HttpClient {

    @JvmField
    val DEFAULT_AGENT = "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersion}" +
        " (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, " +
        "${if (LiquidBounce.IN_DEVELOPMENT) "dev" else "release"}, ${System.getProperty("os.name")})"

    /**
     * Unfortunately, Lunar Client uses OkHttp 4.12.0 which does not have [Headers.EMPTY]
     */
    @Deprecated("Use Headers.EMPTY instead when Lunar Client updates OkHttp to 5.10 or newer.")
    @JvmField
    val EMPTY_HEADERS = Headers.Builder().build()

    object MediaTypes {
        @JvmField
        val JSON = "application/json; charset=utf-8".toMediaType()

        @JvmField
        val FORM = "application/x-www-form-urlencoded".toMediaType()

        @JvmField
        val IMAGE_PNG = "image/png".toMediaType()

        @JvmField
        val OCTET_STREAM = "application/octet-stream".toMediaType()
    }

    private val defaultClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(Util.nonCriticalIoPool().service))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true).apply {
            try {
                val file = File(
                    System.getProperty("java.io.tmpdir"),
                    "${LiquidBounce.CLIENT_NAME.lowercase(Locale.ROOT)}_http_cache",
                )
                file.mkdirs()
                cache(Cache(file, 128L shl 20))
            } catch (e: IOException) {
                logger.error("Failed to initialize cache directory for HTTP client", e)
            }
        }
        .addInterceptor(CacheBlacklistInterceptor(setOf("localhost", "127.0.0.1")))
        .addInterceptor(DefaultHeaderInterceptor("User-Agent", DEFAULT_AGENT, skipIfExists = true))
        .build().also {
            McefFileUtils.setOkHttpClient(it)
            Authlib.client = it
        }

    /**
     * This interceptor rejects all non-2xx responses
     */
    private val clientHttpApiInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            val response = chain.proceed(request)

            if (response.isSuccessful) {
                response
            } else {
                // Response is not successful (code is not 2xx)
                throw HttpException(
                    enumValueOf(request.method),
                    request.url.toString(), response.code, response.body.string()
                )
            }
        } catch (e: IOException) {
            // Failed to request
            logger.error("Failed to execute request ${request.method} ${request.url})", e)
            throw e
        }
    }

    /**
     * API client
     */
    @get:JvmStatic
    val client = defaultClient.newBuilder()
        .addInterceptor(clientHttpApiInterceptor)
        .build()

    @Suppress("LongParameterList")
    suspend fun request(
        url: String,
        method: HttpMethod,
        agent: String = DEFAULT_AGENT,
        headers: Headers.Builder.() -> Unit = {},
        body: RequestBody? = null,
        progressListener: OkHttpProgressInterceptor.ProgressListener? = null
    ): Response {
        val request = Request.Builder()
            .url(url)
            .method(method.name, body)
            .headers(Headers.Builder().apply(headers).build())
            .header("User-Agent", agent)
            .build()

        return if (progressListener == null) {
            client.newCall(request).executeAsync()
        } else {
            client.newBuilder()
                .addNetworkInterceptor(OkHttpProgressInterceptor(progressListener))
                .build()
                .newCall(request).executeAsync()
        }
    }

    suspend fun download(
        url: String,
        file: File,
        agent: String = DEFAULT_AGENT,
        progressListener: OkHttpProgressInterceptor.ProgressListener? = null
    ) = request(url, HttpMethod.GET, agent, progressListener = progressListener).toFile(file)

    // For Java and JS
    @JvmStatic
    fun Call.sendAsync(): CompletableFuture<Response> {
        val future = CompletableFuture<Response>().exceptionally { throwable ->
            if (throwable is CancellationException) this.cancel()
            throw throwable
        }
        this.enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (!future.complete(response)) response.close()
                }

                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(e)
                }
            }
        )
        return future
    }

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
        NativeImage::class.java -> body.byteStream().toNativeImage() as T
        else -> body.charStream().readJson<T>()
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

fun String.asForm() = toRequestBody(HttpClient.MediaTypes.FORM)

class HttpException(val method: HttpMethod, val url: String, val code: Int, val content: String)
    : Exception("${method.name} $url failed with code $code: $content")
