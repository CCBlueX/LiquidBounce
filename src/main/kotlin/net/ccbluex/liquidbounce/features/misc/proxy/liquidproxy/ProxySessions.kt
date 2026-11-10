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
package net.ccbluex.liquidbounce.features.misc.proxy.liquidproxy

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySavedIp
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySession
import net.ccbluex.liquidbounce.api.services.liquidproxy.LiquidProxyApi
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * What the logged-in account played through LiquidProxy.
 */
internal object ProxySessions {

    private const val LIMIT = 20
    private val CLOSE_TIMEOUT = 5.seconds
    private val CLOSE_CHECK_INTERVAL = 500.milliseconds

    private suspend fun oauthSession() = LiquidProxy.account?.takeSession()
        ?: throw LiquidProxyException("liquidbounce.liquidproxy.error.notLoggedIn")

    /**
     * The latest sessions, running ones first.
     */
    suspend fun latest(): List<ProxySession> = LiquidProxyApi.getSessions(oauthSession(), LIMIT)

    suspend fun savedIps(): List<ProxySavedIp> = LiquidProxyApi.getSavedIps(oauthSession())

    /**
     * Ends a running session and waits until LiquidProxy has closed it.
     */
    suspend fun end(id: String) {
        LiquidProxyApi.closeSession(oauthSession(), id)
        withTimeoutOrNull(CLOSE_TIMEOUT) {
            while (LiquidProxyApi.getSession(oauthSession(), id).connected) {
                delay(CLOSE_CHECK_INTERVAL)
            }
        }
    }

    /**
     * Makes the player of a session get a new IP on the next join to its server.
     *
     * @return the session
     */
    suspend fun changeIp(id: String): ProxySession {
        val session = LiquidProxyApi.getSession(oauthSession(), id)
        val savedIp = session.savedIp ?: throw LiquidProxyException("liquidbounce.liquidproxy.error.noSavedIp")

        LiquidProxyApi.releaseSavedIp(oauthSession(), savedIp)
        return session
    }

}
