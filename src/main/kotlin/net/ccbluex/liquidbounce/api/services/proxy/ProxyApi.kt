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
package net.ccbluex.liquidbounce.api.services.proxy

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.addAuth
import net.ccbluex.liquidbounce.api.models.proxy.ProxyLocation
import net.ccbluex.liquidbounce.api.models.proxy.ProxySubscription

/**
 * API for LiquidProxy-related endpoints
 */
object ProxyApi : BaseApi(config.apiEndpointV3) {

    suspend fun getSubscription(session: OAuthSession) = get<ProxySubscription>(
        "/proxy/subscription",
        headers = { addAuth(session) }
    )

    suspend fun getLocations() = get<Set<ProxyLocation>>("/proxy/locations")

}

