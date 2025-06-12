/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.api.models.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.user.UserInformation
import net.ccbluex.liquidbounce.api.services.auth.OAuthClient
import net.ccbluex.liquidbounce.api.services.user.UserApi
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import java.util.*


/**
 * Represents a client account that is used to authenticate with the LiquidBounce API.
 * It might hold additional information that can be obtained from the API.
 */
data class ClientAccount(
    private var session: OAuthSession? = null,
    @Exclude
    var userInformation: UserInformation? = null,
    @Exclude
    var cosmetics: Set<Cosmetic>? = null
) {
    private suspend fun takeSession(): OAuthSession = session?.takeIf { !it.accessToken.isExpired() } ?: run {
        renew()
        session ?: error("No session")
    }

    suspend fun updateInfo(): Unit = withContext(Dispatchers.IO) {
        userInformation = UserApi.getUserInformation(takeSession())
    }

    suspend fun updateCosmetics(): Unit = withContext(Dispatchers.IO) {
        cosmetics = UserApi.getCosmetics(takeSession())
    }

    suspend fun transferTemporaryOwnership(uuid: UUID): Unit = withContext(Dispatchers.IO) {
        UserApi.transferTemporaryOwnership(takeSession(), uuid)
    }

    suspend fun renew() = withContext(Dispatchers.IO) {
        session = OAuthClient.renewToken(session ?: error("No session"))
    }

    companion object {
        val EMPTY_ACCOUNT = ClientAccount(null, null, null)
    }
}
