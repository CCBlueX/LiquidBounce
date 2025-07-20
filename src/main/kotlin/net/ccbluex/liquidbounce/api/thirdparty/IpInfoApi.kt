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
package net.ccbluex.liquidbounce.api.thirdparty

import net.ccbluex.liquidbounce.api.core.AsyncLazy
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * An implementation for the ipinfo.io API including
 * keeping track of the current IP address.
 */
object IpInfoApi : BaseApi("https://ipinfo.io") {

    /**
     * Information about the current IP address of the user. This can change depending on if the
     * user is using a proxy through the Proxy Manager.
     */
    val current: IpData?
        get() = ProxyManager.currentProxy?.ipInfo ?: original

    /**
     * Information about the current IP address of the user. This does not change during use.
     *
     * We are only interested in the [IpData.country] for displaying the country in the GUI,
     * which is unlikely to change, even when changing the IP address. This could happen when using a VPN,
     * but it's not that important to keep this updated all the time.
     */
    val original: IpData? by AsyncLazy {
        runCatching { own() }.onFailure {
            logger.error("Failed to get own IP address", it)
        }.getOrNull()
    }

    suspend fun own() = get<IpData>("/json")
    suspend fun someoneElse(ip: String) = get<IpData>("/$ip/json")

    /**
     * Represents information about an IP address
     */
    data class IpData(
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

}
