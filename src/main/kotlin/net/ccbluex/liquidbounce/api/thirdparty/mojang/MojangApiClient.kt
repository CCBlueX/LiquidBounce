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

package net.ccbluex.liquidbounce.api.thirdparty.mojang

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import net.ccbluex.liquidbounce.api.interceptors.TokenInterceptor
import net.ccbluex.liquidbounce.api.thirdparty.mojang.service.MinecraftServicesApi
import net.ccbluex.liquidbounce.api.thirdparty.mojang.service.MojangApi
import net.ccbluex.liquidbounce.api.thirdparty.mojang.service.SessionServerApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * The Microsoft/Xbox Live/XSTS token chain is not part of this client - MinecraftAuth implements it,
 * see [net.ccbluex.liquidbounce.features.account.MicrosoftAccount].
 *
 * ```kotlin
 * val client = MojangApiClient.Builder().httpClient(HttpClient.defaultClient).build()
 * ```
 */
class MojangApiClient internal constructor(
    val mojangApi: MojangApi,
    val mcServicesApi: MinecraftServicesApi,
    val sessionServerApi: SessionServerApi,
) {
    private companion object {
        private val apiGson by lazy {
            GsonBuilder()
                .setStrictness(Strictness.LENIENT)
                .serializeNulls()
                .create()
        }
    }

    class Builder {
        private var gson: Gson? = null

        private var baseHttpClient: OkHttpClient? = null

        private var tokenProvider: () -> String? = { null }

        fun gson(gson: Gson) = apply { this.gson = gson }

        fun httpClient(client: OkHttpClient) = apply { this.baseHttpClient = client }

        fun tokenProvider(provider: () -> String?) = apply { this.tokenProvider = provider }

        fun build(): MojangApiClient {
            val tokenInterceptor = TokenInterceptor(tokenProvider)

            val baseClient = requireNotNull(baseHttpClient) { "No base OkHttpClient was set" }
            val authenticatedClient = baseClient.newBuilder()
                .addInterceptor(tokenInterceptor)
                .build()

            val converter = GsonConverterFactory.create(gson ?: apiGson)

            fun retrofit(baseUrl: String, client: OkHttpClient = baseClient): Retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(converter)
                .build()

            return MojangApiClient(
                mojangApi = retrofit("https://api.mojang.com/")
                    .create(MojangApi::class.java),

                mcServicesApi = retrofit("https://api.minecraftservices.com/", authenticatedClient)
                    .create(MinecraftServicesApi::class.java),

                sessionServerApi = retrofit("https://sessionserver.mojang.com/")
                    .create(SessionServerApi::class.java),
            )
        }
    }
}
