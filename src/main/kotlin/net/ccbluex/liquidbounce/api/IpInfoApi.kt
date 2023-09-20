/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.api

import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.io.HttpClient

object IpInfoApi {

    private const val API_URL = "https://ipinfo.io/json"

    var localIpInfo: IpInfo? = requestIpInfo()
        private set

    /**
     * Refresh local IP info
     */
    fun refreshLocalIpInfo() {
        localIpInfo = requestIpInfo()
    }

    /**
     * Request IP info from API
     *
     * todo: add support for proxy
     */
    private fun requestIpInfo(): IpInfo? = runCatching {
        endpointRequest<IpInfo>(API_URL)
    }.getOrNull()

    /**
     * Request endpoint and parse JSON to data class
     */
    private inline fun <reified T> endpointRequest(endpoint: String): T? = decode(makeEndpointRequest(endpoint))

    /**
     * Request to endpoint with custom agent and session token
     */
    private fun makeEndpointRequest(endpoint: String) = HttpClient.get(endpoint)
}

data class IpInfo(
    val ip: String?,
    val hostname: String?,
    val city: String?,
    val region: String?,
    val country: String?,
    val loc: String?,
    val org: String?,
    val postal: String?,
    val timezone: String?
)
