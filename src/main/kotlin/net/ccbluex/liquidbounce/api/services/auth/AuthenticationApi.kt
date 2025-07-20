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
package net.ccbluex.liquidbounce.api.services.auth

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_BASE_URL
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.core.asForm
import net.ccbluex.liquidbounce.api.models.auth.TokenResponse

/**
 * API for OAuth authentication
 */
object AuthenticationApi : BaseApi(AUTH_BASE_URL) {
    suspend fun exchangeToken(
        clientId: String,
        code: String,
        codeVerifier: String,
        redirectUri: String
    ) = post<TokenResponse>(
        "/token/",
        ("client_id=$clientId&code=$code&code_verifier=$codeVerifier&" +
            "grant_type=authorization_code&redirect_uri=$redirectUri").asForm()
    )

    suspend fun refreshToken(
        clientId: String,
        refreshToken: String
    ) = post<TokenResponse>(
        "/token/",
        "client_id=$clientId&refresh_token=$refreshToken&grant_type=refresh_token".asForm()
    )
}
