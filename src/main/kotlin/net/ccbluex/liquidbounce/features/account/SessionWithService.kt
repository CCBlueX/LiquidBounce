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

package net.ccbluex.liquidbounce.features.account

import net.minecraft.client.User
import java.util.Optional
import java.util.UUID

@Suppress("LongParameterList")
class SessionWithService(
    username: String,
    uuid: UUID,
    accessToken: String,
    xuid: Optional<String>,
    clientId: Optional<String>,
    val service: AccountService
) : User(username, uuid, accessToken, xuid, clientId) {

    companion object {
        fun getService(session: User) = when {
            session is SessionWithService -> session.service
            session.couldBeOnline() -> AccountService.MICROSOFT
            else -> AccountService.CRACKED
        }
    }

}

/**
 * Checks if the session is online by checking the account type and if we have a valid access token.
 */
fun User.couldBeOnline() = accessToken.startsWith("eyJra") && accessToken.length >= 13
