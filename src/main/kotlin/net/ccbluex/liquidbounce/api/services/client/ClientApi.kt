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

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.API_EDITION
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.models.client.Build
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

object ClientApi {

    private val service = config.retrofit.create<Service>()

    /**
     * The newest build of [edition], with [release] its newest release. `null` while it has none.
     */
    suspend fun requestNewestBuildEndpoint(edition: String = API_EDITION, release: Boolean = false): Build? =
        service.builds(edition, release = if (release) true else null, limit = 1).items.firstOrNull()

    internal interface Service {
        @GET("builds")
        suspend fun builds(
            @Query("edition") edition: String,
            @Query("release") release: Boolean?,
            @Query("limit") limit: Int,
        ): PaginatedResponse<Build>
    }

}
