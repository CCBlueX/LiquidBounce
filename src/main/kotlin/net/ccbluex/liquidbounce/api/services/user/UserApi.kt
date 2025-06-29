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
package net.ccbluex.liquidbounce.api.services.user

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.core.toRequestBody
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.addAuth
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.user.UserInformation
import java.util.*

/**
 * API for user-related endpoints that require authentication
 */
object UserApi : BaseApi(config.apiEndpointV3) {

    suspend fun getUserInformation(session: OAuthSession) = get<UserInformation>(
        "/oauth/user",
        headers = { addAuth(session) }
    )

    suspend fun getCosmetics(session: OAuthSession) = get<Set<Cosmetic>>(
        "/cosmetics/self",
        headers = { addAuth(session) }
    )

    suspend fun transferTemporaryOwnership(session: OAuthSession, uuid: UUID) = put<Unit>(
        "/cosmetics/self",
        JsonObject().apply {
            addProperty("uuid", uuid.toString())
        }.toRequestBody(),
        headers = { addAuth(session) }
    )
}
