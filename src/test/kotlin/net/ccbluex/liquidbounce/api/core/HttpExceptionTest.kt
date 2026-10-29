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

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * A non-2xx answer must reach the caller as [HttpException], not as the `canceled due to ...` wrapper
 * OkHttp substitutes for every other throwable.
 */
class HttpExceptionTest {

    private val refused = HttpException(HttpMethod.GET, URL, 404, """{"error":"gone"}""")

    private val client = OkHttpClient.Builder()
        .addInterceptor { throw refused }
        .build()

    @Test
    fun `an async call surfaces the rejection as itself`() = runBlocking {
        val thrown = assertFailsWith<HttpException> {
            client.newCall(Request.Builder().url(URL).build()).executeAsync()
        }

        assertSame(refused, thrown)
    }

    @Test
    fun `a suspending retrofit call surfaces the rejection as itself`() = runBlocking {
        val service = Retrofit.Builder()
            .baseUrl("https://api.invalid/")
            .callFactory(client)
            .build()
            .create<Service>()

        val thrown = assertFailsWith<HttpException> { service.get() }

        assertSame(refused, thrown)
    }

    private interface Service {
        @GET("x")
        suspend fun get()
    }

    private companion object {
        const val URL = "https://api.invalid/x"
    }

}
