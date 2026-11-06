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
package net.ccbluex.liquidbounce.api.services.user

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.core.toRequestBody
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.authorization
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.user.UserInformation
import okhttp3.RequestBody
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import java.util.UUID

/**
 * API for user-related endpoints that require authentication
 */
object UserApi {

    private val service = config.retrofitApiV3.create<Service>()

    suspend fun getUserInformation(session: OAuthSession) = service.userInformation(session.authorization)

    suspend fun getCosmetics(session: OAuthSession) = service.cosmetics(session.authorization)

    suspend fun transferTemporaryOwnership(session: OAuthSession, uuid: UUID) = service.transferOwnership(
        JsonObject().apply { addProperty("uuid", uuid.toString()) }.toRequestBody(),
        session.authorization,
    )

    internal interface Service {
        @GET("oauth/user")
        suspend fun userInformation(@Header("Authorization") authorization: String): UserInformation

        @GET("cosmetics/self")
        suspend fun cosmetics(@Header("Authorization") authorization: String): Set<Cosmetic>

        @PUT("cosmetics/self")
        suspend fun transferOwnership(
            @Body body: RequestBody,
            @Header("Authorization") authorization: String,
        )
    }

}
