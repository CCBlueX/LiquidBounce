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
package net.ccbluex.liquidbounce.api.services.client

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.API_BRANCH
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.models.client.Build
import net.ccbluex.liquidbounce.api.models.client.MessageOfTheDay
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path

object ClientApi {

    private val service = config.retrofitApiV1.create<Service>()

    suspend fun requestNewestBuildEndpoint(branch: String = API_BRANCH, release: Boolean = false) =
        if (release) service.newestReleaseBuild(branch) else service.newestBuild(branch)

    suspend fun requestMessageOfTheDayEndpoint(branch: String = API_BRANCH) = service.messageOfTheDay(branch)

    internal interface Service {
        @GET("version/newest/{branch}")
        suspend fun newestBuild(@Path("branch") branch: String): Build

        @GET("version/newest/{branch}/release")
        suspend fun newestReleaseBuild(@Path("branch") branch: String): Build

        @GET("client/{branch}/motd")
        suspend fun messageOfTheDay(@Path("branch") branch: String): MessageOfTheDay
    }

}
