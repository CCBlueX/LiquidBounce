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
package net.ccbluex.liquidbounce.api.services.auth

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_BASE_URL
import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.core.apiRetrofit
import net.ccbluex.liquidbounce.api.core.asForm
import net.ccbluex.liquidbounce.api.models.auth.TokenResponse
import okhttp3.RequestBody
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API for OAuth authentication
 */
object AuthenticationApi {

    private val service = apiRetrofit(AUTH_BASE_URL).create<Service>()

    suspend fun exchangeToken(
        clientId: String,
        code: String,
        codeVerifier: String,
        redirectUri: String
    ) = service.token(
        ("client_id=$clientId&code=$code&code_verifier=$codeVerifier&" +
            "grant_type=authorization_code&redirect_uri=$redirectUri").asForm()
    )

    suspend fun refreshToken(
        clientId: String,
        refreshToken: String
    ) = service.token("client_id=$clientId&refresh_token=$refreshToken&grant_type=refresh_token".asForm())

    internal interface Service {
        @POST("token/")
        suspend fun token(@Body body: RequestBody): TokenResponse
    }

}

val HttpException.isInvalidGrant: Boolean
    get() = runCatching { JsonParser.parseString(content).asJsonObject["error"]?.asString }
        .getOrNull() == "invalid_grant"
