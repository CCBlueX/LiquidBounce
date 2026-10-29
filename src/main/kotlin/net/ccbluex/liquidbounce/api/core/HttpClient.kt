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

import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.converters.BufferConverterFactory
import net.ccbluex.liquidbounce.api.converters.NativeImageConverterFactory
import net.ccbluex.liquidbounce.api.interceptors.DefaultHeaderInterceptor
import net.ccbluex.liquidbounce.api.interceptors.SkipCacheInterceptor
import net.ccbluex.liquidbounce.api.interceptors.whenUrl
import net.ccbluex.liquidbounce.api.thirdparty.mojang.MojangApiClient
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.apiGson
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.interop.middleware.AuthConfig
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.CommonCoroutineExceptionHandler
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val renderScope = CoroutineScope(
    Dispatchers.Main + SupervisorJob() + CommonCoroutineExceptionHandler.Default
)

val ioScope = CoroutineScope(
    Dispatchers.IO + SupervisorJob() + CommonCoroutineExceptionHandler.Default
)

fun withScope(block: suspend CoroutineScope.() -> Unit) = ioScope.launch(block = block)

object HttpClient {

    @JvmField
    val DEFAULT_AGENT = "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersion}" +
        " (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, " +
        "${if (LiquidBounce.IN_DEVELOPMENT) "dev" else "release"}, ${System.getProperty("os.name")})"

    private fun String.isLocalHost() = this == "localhost" || this == "127.0.0.1"

    object MediaTypes {
        @JvmField
        val TEXT_PLAIN = "text/plain; charset=utf-8".toMediaType()

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
        .dispatcher(
            Dispatcher(
                Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual().name("OkHttpClient Dispatcher ", 0L).factory()
                )
            )
        )
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
        .addInterceptor(SkipCacheInterceptor { it.isLocalHost() })
        .addInterceptor(DefaultHeaderInterceptor(HttpHeaders.UserAgent) { DEFAULT_AGENT })
        .proxy(java.net.Proxy.NO_PROXY)
        .build()

    /**
     * This interceptor rejects all non-2xx responses
     */
    private val clientHttpApiInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // Failed to request
            logger.error("Failed to execute request ${request.method} ${request.url})", e)
            throw e
        }

        if (response.isSuccessful) {
            response
        } else {
            // Response is not successful (code is not 2xx), its body becomes the exception's content
            throw HttpException(
                enumValueOf(request.method),
                request.url.toString(), response.code, response.body.string()
            )
        }
    }

    /**
     * API client
     */
    @get:JvmStatic
    val client = defaultClient.newBuilder()
        .addInterceptor(clientHttpApiInterceptor)
        // Credentials are scoped to the hosts they belong to. Registered as network interceptors, so they
        // are re-evaluated for every redirect hop and cannot follow a redirect to a foreign host.
        .addNetworkInterceptor(
            DefaultHeaderInterceptor(ApiConfig.SESSION_TOKEN_HEADER) {
                checkNotNull(ApiConfig.initializedConfig) { "API config is not initialized yet" }.sessionToken
            }.whenUrl { url ->
                val config = ApiConfig.initializedConfig
                config != null && url.host == config.url.host
            }
        )
        .addNetworkInterceptor(
            DefaultHeaderInterceptor(HttpHeaders.Cookie, {
                "${AuthConfig.AUTH_COOKIE_NAME}=${ClientInteropServer.AUTH_CODE}"
            }, false).whenUrl { it.host.isLocalHost() && it.port == ClientInteropServer.PORT }
        )
        .build()

    @get:JvmStatic
    val mojangApiClient = MojangApiClient.Builder()
        .gson(interopGson)
        .httpClient(this.defaultClient)
        .tokenProvider { mc.user.accessToken }
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
            .header(HttpHeaders.UserAgent, agent)
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
    ) = withContext(Dispatchers.IO) {
        request(url, HttpMethod.GET, agent, progressListener = progressListener).bodyToFile(file)
    }

    /** For Java and JS. Cancelling the returned future also cancels the HTTP call. */
    @JvmStatic
    fun Call.sendAsync(): CompletableFuture<Response> = enqueueAsFuture()

}

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD
}

/**
 * Retrofit over [HttpClient.client] for one of our APIs.
 *
 * Response bodies are read and converted in the OkHttp callback (see [retrofit2.OkHttpCall.enqueue]),
 * so awaiting a call never reads or deserializes a body on the caller's thread.
 */
internal fun apiRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
    .baseUrl(if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/")
    .callFactory(HttpClient.client)
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(BufferConverterFactory)
    .addConverterFactory(NativeImageConverterFactory)
    .addConverterFactory(GsonConverterFactory.create(apiGson))
    .build()

