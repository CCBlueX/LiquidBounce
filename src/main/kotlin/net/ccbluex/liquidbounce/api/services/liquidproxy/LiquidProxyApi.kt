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
package net.ccbluex.liquidbounce.api.services.liquidproxy

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.api.core.toRequestBody
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.addAuth
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySession
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySubscription
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySubscriptionType
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse

/**
 * LiquidProxy endpoints of the LiquidBounce API. Everything but [getLocations] acts on the logged-in account.
 */
object LiquidProxyApi : BaseApi(config.apiEndpointV3) {

    suspend fun getLocations() = HttpClient.request("${config.apiEndpointV2}/proxy/locations", HttpMethod.GET)
        .parse<List<ProxyLocation>>()

    /**
     * @throws net.ccbluex.liquidbounce.api.core.HttpException with code 404 without a subscription
     */
    suspend fun getSubscription(session: OAuthSession) = get<ProxySubscription>(
        "/proxy/subscription",
        headers = { addAuth(session) }
    )

    suspend fun getSubscriptionTypes(session: OAuthSession) = get<List<ProxySubscriptionType>>(
        "/proxy/subscription-types",
        headers = { addAuth(session) }
    )

    suspend fun getSessions(session: OAuthSession, limit: Int) = get<PaginatedResponse<ProxySession>>(
        "/proxy/sessions?page=1&limit=$limit",
        headers = { addAuth(session) }
    )

    /**
     * Gives [username] a new IP on its next join.
     *
     * @return false when one is already waiting for that join
     */
    suspend fun requestNewIp(session: OAuthSession, username: String): Boolean {
        val response = HttpClient.request(
            "$baseUrl/proxy/refresh/$username",
            HttpMethod.POST,
            headers = {
                add("X-Session-Token", config.sessionToken)
                addAuth(session)
            },
            body = JsonObject().toRequestBody()
        )
        return response.use { it.code != ACCEPTED }
    }

    suspend fun killConnection(session: OAuthSession, connId: String) = post<Unit>(
        "/proxy/kill-connection/$connId",
        JsonObject().toRequestBody(),
        headers = { addAuth(session) }
    )

    private const val ACCEPTED = 202

}
