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

import net.ccbluex.liquidbounce.api.thirdparty.mojang.model.ProfileIdName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints on [api.mojang.com](https://api.mojang.com).
 */
interface MojangApi {

    /** Returns 404 if no player exists. */
    @GET("users/profiles/minecraft/{username}")
    suspend fun fetchUuidByUsername(@Path("username") username: String): ProfileIdName

    @GET("minecraft/profile/lookup/name/{username}")
    suspend fun lookupUuidByName(@Path("username") username: String): ProfileIdName

    /** Max 10 names. Missing ones are omitted from the response. */
    @POST("profiles/minecraft")
    suspend fun fetchProfiles(@Body names: List<String>): List<ProfileIdName>

    @POST("minecraft/profile/lookup/bulk/byname")
    suspend fun lookupProfilesBulk(@Body names: List<String>): List<ProfileIdName>
}
