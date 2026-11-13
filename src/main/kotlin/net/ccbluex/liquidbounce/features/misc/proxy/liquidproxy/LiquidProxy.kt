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
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyCredentials
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyEntitlement
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyLocation
import net.ccbluex.liquidbounce.api.models.liquidproxy.ProxyNetwork
import net.ccbluex.liquidbounce.api.services.liquidproxy.LiquidProxyApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.lang.translation
import java.io.IOException

/**
 * LiquidProxy, set up from the LiquidBounce account it was bought with.
 *
 * A connection is an ordinary [Proxy] in [ProxyManager]: its host picks the location, and the suffix of its
 * username the network.
 */
internal object LiquidProxy : Config("liquidProxy"), EventListener {

    const val SOCKS_PORT = 1080

    /**
     * Not a network of its own: LiquidProxy picks the one each server needs.
     */
    private val SMART = ProxyNetwork("smart", "Smart", usernameSuffix = 0)

    var network by text("Network", SMART.code)
        private set
    var forwardAuthentication by boolean("ForwardAuthentication", true)
        private set

    /**
     * The code of the location chosen last
     */
    var location by text("Location", "")
        private set

    /**
     * The proxy set up last. While it is the current one, it is ours to keep up to date and to end, unlike a
     * proxy to LiquidProxy that the user added by hand.
     */
    private var connection by value("Connection", Proxy.NONE, valueType = ValueType.PROXY)

    private val isConnectionCurrent
        get() = ProxyManager.currentProxy?.isSameAs(connection) == true

    /**
     * LiquidProxy as the logged-in account has it.
     *
     * @param networks what a connection can go through, [SMART] first
     */
    class Subscription(
        val entitlement: ProxyEntitlement,
        val networks: List<ProxyNetwork>,
        private val credentials: ProxyCredentials,
    ) {

        /**
         * The chosen network, or [SMART] when the subscription does not include it.
         */
        val network
            get() = networks.find { it.code == LiquidProxy.network } ?: SMART

        fun proxy(location: ProxyLocation) = Proxy(
            host = location.host,
            port = SOCKS_PORT,
            credentials = Proxy.Credentials("${credentials.username}.${network.usernameSuffix}", credentials.password),
            type = Proxy.Type.SOCKS5,
            forwardAuthentication = forwardAuthentication,
        )

    }

    private val subscriptionLock = Mutex()
    @Volatile
    private var subscription: Subscription? = null
    @Volatile
    private var locations = emptyList<ProxyLocation>()

    init {
        ConfigSystem.root(this)
    }

    val account
        get() = ClientAccountManager.clientAccount.takeIf { it != ClientAccount.EMPTY_ACCOUNT }

    /**
     * The subscription of the logged-in account, `null` without one. It is loaded once, and again on [refresh].
     */
    suspend fun subscription(refresh: Boolean = false): Subscription? = subscriptionLock.withLock {
        if (subscription == null || refresh) {
            subscription = loadSubscription()
            updateConnection()
        }
        subscription
    }

    private suspend fun activeSubscription() = subscription()?.takeIf { it.entitlement.isActive }
        ?: throw LiquidProxyException("liquidbounce.liquidproxy.error.inactive")

    private suspend fun loadSubscription(): Subscription? {
        val session = account?.takeSession() ?: return null
        val entitlement = LiquidProxyApi.getEntitlement(session) ?: return null
        val networks = LiquidProxyApi.getNetworks().associateBy { it.code }

        return Subscription(
            entitlement,
            listOf(SMART) + entitlement.networks.mapNotNull(networks::get),
            LiquidProxyApi.getCredentials(session),
        )
    }

    /**
     * Carries a network the subscription no longer includes, or a new proxy login, over to our connection.
     */
    private suspend fun updateConnection() {
        val subscription = subscription?.takeIf { it.entitlement.isActive } ?: return
        val location = connectedLocation()?.takeIf { isConnectionCurrent } ?: return

        val proxy = subscription.proxy(location)
        if (!proxy.isSameAs(connection) || proxy.credentials?.password != connection.credentials?.password) {
            connect(location, proxy)
        }
    }

    /**
     * The locations, loaded once, and again on [refresh]. A refresh that fails keeps the ones from before.
     */
    suspend fun locations(refresh: Boolean = false): List<ProxyLocation> {
        if (locations.isEmpty() || refresh) {
            try {
                locations = LiquidProxyApi.getLocations()
            } catch (e: IOException) {
                if (locations.isEmpty()) {
                    throw e
                }
            }
        }
        return locations
    }

    /**
     * The location the current proxy goes through, including one the user added by hand.
     */
    suspend fun connectedLocation(): ProxyLocation? {
        val host = ProxyManager.currentProxy?.host ?: return null
        return locations().find { it.host.equals(host, ignoreCase = true) }
    }

    suspend fun connect(code: String): ProxyLocation {
        val location = locations().find { it.code == code }
            ?: throw LiquidProxyException("liquidbounce.liquidproxy.error.unknownLocation")
        connect(location, activeSubscription().proxy(location))
        return location
    }

    private fun connect(location: ProxyLocation, proxy: Proxy) {
        connection = proxy
        this.location = location.code
        ConfigSystem.store(this)
        ProxyManager.proxy = proxy
    }

    /**
     * @return false when there was nothing to disconnect from
     */
    suspend fun disconnect(): Boolean {
        connectedLocation() ?: return false
        ProxyManager.proxy = Proxy.NONE
        return true
    }

    /**
     * Changes the given settings, for the current connection as well.
     */
    suspend fun configure(network: String?, forwardAuthentication: Boolean?) {
        val subscription = activeSubscription()
        if (network != null) {
            if (subscription.networks.none { it.code == network }) {
                throw LiquidProxyException("liquidbounce.liquidproxy.error.network")
            }
            this.network = network
        }
        if (forwardAuthentication != null) {
            this.forwardAuthentication = forwardAuthentication
        }
        ConfigSystem.store(this)

        val location = connectedLocation() ?: return
        connect(location, subscription.proxy(location))
    }

    @Suppress("unused")
    private val loginHandler = handler<UserLoggedInEvent> {
        subscription = null
    }

    @Suppress("unused")
    private val logoutHandler = handler<UserLoggedOutEvent> {
        subscription = null
        if (isConnectionCurrent) {
            ProxyManager.proxy = Proxy.NONE
        }
    }

}

/**
 * Something LiquidProxy can't do for the player, said in their language.
 */
internal class LiquidProxyException(translationKey: String) : Exception(translation(translationKey).string)
