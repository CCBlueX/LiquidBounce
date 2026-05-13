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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.services.auth.OAuthClient.startAuth
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.netty.http.routing.RoutingContext

// GET /api/v1/client/user
suspend fun RoutingContext.getUser() {
    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
        unauthorized("Not logged in")
    }

    val userInformation = clientAccount.userInformation ?: run {
        clientAccount.updateInfo()
        clientAccount.userInformation
    }

    respond(interopGson.toJsonTree(userInformation))
}

// POST /api/v2/client/user/login
suspend fun RoutingContext.loginUser() {
    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount != ClientAccount.EMPTY_ACCOUNT) {
        badRequest("Already logged in")
    }

    val account = startAuth { url -> browseUrl(url) }.apply {
        updateInfo()
    }
    ClientAccountManager.clientAccount = account
    ConfigSystem.store(ClientAccountManager)
    EventManager.callEvent(UserLoggedInEvent)

    respond(interopGson.toJsonTree(account.userInformation))
}

// POST /api/v2/client/user/logout
fun RoutingContext.logoutUser() {
    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
        badRequest("Not logged in")
    }

    ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
    ConfigSystem.store(ClientAccountManager)
    EventManager.callEvent(UserLoggedOutEvent)
    respondNoContent()
}
