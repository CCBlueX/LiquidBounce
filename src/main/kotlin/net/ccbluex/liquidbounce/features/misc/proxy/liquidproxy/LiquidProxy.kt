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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySession
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySubscription
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxySubscriptionType
import net.ccbluex.liquidbounce.api.services.liquidproxy.LiquidProxyApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import java.io.IOException

/**
 * LiquidProxy, set up from the LiquidBounce account it is bought with.
 *
 * A connection is an ordinary [Proxy] in [ProxyManager]: the node's host picks the location, and the level
 * appended to the username picks the plan.
 */
internal object LiquidProxy : Config("liquidProxy"), EventListener {

    const val SMART_LEVEL = 0
    private const val SOCKS_PORT = 1080
    private const val SESSION_LIMIT = 20

    /** Smart is no plan of its own: the node picks the level each server needs. */
    private val SMART = ProxySubscriptionType(SMART_LEVEL, "Smart", null)

    var level by int("Level", SMART_LEVEL, 0..255)
    var forwardAuthentication by boolean("ForwardAuthentication", true)
    var location by text("Location", "")

    private val mutex = Mutex()
    @Volatile
    private var subscription: ProxySubscription? = null
    @Volatile
    private var types = emptyList<ProxySubscriptionType>()
    @Volatile
    private var locations: List<ProxyLocation>? = null

    init {
        ConfigSystem.root(this)
    }

    private val account
        get() = ClientAccountManager.clientAccount.takeIf { it != ClientAccount.EMPTY_ACCOUNT }

    val isLoggedIn
        get() = account != null

    private suspend fun session(): OAuthSession = account?.takeSession() ?: error("Not logged in")

    /**
     * The subscription of the logged-in account, or null without one.
     */
    suspend fun subscription(refresh: Boolean = false): ProxySubscription? = mutex.withLock {
        if (!refresh && subscription != null) {
            return subscription
        }

        val session = account?.takeSession() ?: return null
        val subscription = try {
            LiquidProxyApi.getSubscription(session)
        } catch (e: IOException) {
            if (e.httpException?.code != NOT_FOUND) {
                throw e
            }
            null
        }

        types = subscription?.let { LiquidProxyApi.getSubscriptionTypes(session) }.orEmpty()
        this.subscription = subscription
        subscription
    }

    /**
     * Smart and the plans the subscription includes.
     */
    val plans
        get() = listOf(SMART) + types.filter { it.level != SMART_LEVEL }.sortedBy { it.level }

    /**
     * The chosen plan, or Smart when the subscription no longer includes it.
     */
    val effectiveLevel
        get() = level.takeIf { level -> plans.any { it.level == level } } ?: SMART_LEVEL

    suspend fun locations(): List<ProxyLocation> =
        locations ?: LiquidProxyApi.getLocations().also { locations = it }

    /**
     * The location the current proxy goes through, including one the user added by hand.
     */
    val connectedLocation: ProxyLocation?
        get() {
            val host = ProxyManager.currentProxy?.host ?: return null
            return locations?.find { host == it.address || host.endsWith(".${it.address}") }
        }

    suspend fun connect(code: String) {
        val location = locations().find { it.code == code } ?: error("Unknown location")
        val subscription = subscription() ?: error("You have no LiquidProxy subscription")
        check(subscription.isActive) { "Your LiquidProxy subscription is not active" }

        this.location = code
        ConfigSystem.store(this)
        ProxyManager.proxy = proxy(location, subscription)
    }

    fun disconnect() {
        if (connectedLocation != null) {
            ProxyManager.proxy = Proxy.NONE
        }
    }

    /**
     * Carries a changed plan, setting or password over to the current connection.
     */
    suspend fun refreshConnection() {
        val location = connectedLocation ?: return
        val subscription = subscription()?.takeIf { it.isActive } ?: return
        val proxy = proxy(location, subscription)

        val current = ProxyManager.proxy
        if (current.host != proxy.host || current.forwardAuthentication != proxy.forwardAuthentication ||
            current.credentials?.username != proxy.credentials?.username ||
            current.credentials?.password != proxy.credentials?.password) {
            ProxyManager.proxy = proxy
        }
    }

    fun proxy(location: ProxyLocation, subscription: ProxySubscription): Proxy {
        val level = effectiveLevel
        val plan = plans.first { it.level == level }.name.lowercase().filter { it.isLetterOrDigit() }
        return Proxy(
            host = "$plan.${location.address}",
            port = SOCKS_PORT,
            credentials = Proxy.Credentials("${subscription.username}.$level", subscription.password),
            type = Proxy.Type.SOCKS5,
            forwardAuthentication = forwardAuthentication
        )
    }

    suspend fun sessions(): List<ProxySession> = LiquidProxyApi.getSessions(session(), SESSION_LIMIT).items

    suspend fun endSession(connId: String) = LiquidProxyApi.killConnection(session(), connId)

    /**
     * @return false when [username] already gets a new IP on its next join
     */
    suspend fun requestNewIp(username: String) = LiquidProxyApi.requestNewIp(session(), username)

    @Suppress("unused")
    private val loggedInHandler = handler<UserLoggedInEvent> {
        subscription = null
        types = emptyList()
    }

    @Suppress("unused")
    private val loggedOutHandler = handler<UserLoggedOutEvent> {
        disconnect()
        subscription = null
        types = emptyList()
    }

    private const val NOT_FOUND = 404

}
