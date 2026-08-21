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

package net.ccbluex.liquidbounce.api.thirdparty.mojang.service

import net.ccbluex.liquidbounce.api.thirdparty.mojang.model.JoinServerRequest
import net.ccbluex.liquidbounce.api.thirdparty.mojang.model.SessionProfile
import retrofit2.Response
import retrofit2.http.*

/**
 * Endpoints on [sessionserver.mojang.com](https://sessionserver.mojang.com).
 */
interface SessionServerApi {

    /** Returns 204 if the UUID has no player. */
    @GET("session/minecraft/profile/{uuid}")
    suspend fun fetchProfile(@Path("uuid") uuid: String): Response<SessionProfile>

    /** As above, with signature verification. */
    @GET("session/minecraft/profile/{uuid}")
    suspend fun fetchProfileSigned(
        @Path("uuid") uuid: String,
        @Query("unsigned") unsigned: Boolean = false,
    ): Response<SessionProfile>

    /** Returns 204 on success. */
    @POST("session/minecraft/join")
    suspend fun joinServer(@Body request: JoinServerRequest): Response<Unit>

    /** Returns the profile on success, 204/empty on failure. */
    @GET("session/minecraft/hasJoined")
    suspend fun hasJoined(
        @Query("username") username: String,
        @Query("serverId") serverId: String,
        @Query("ip") ip: String? = null,
    ): Response<SessionProfile>

    /** SHA-1 hashes, one per line. */
    @GET("blockedservers")
    suspend fun getBlockedServers(): String
}
