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
package net.ccbluex.liquidbounce.features.misc.proxy

import java.net.Authenticator
import java.net.PasswordAuthentication

/**
 * Answers credential requests for the current proxy and passes everything else on.
 *
 * JDK SOCKS sockets only ask the default authenticator, so it has to be installed globally.
 */
internal object ProxyAuthenticator : Authenticator() {

    @Volatile
    private var fallback: Authenticator? = null

    @Synchronized
    fun install() {
        val current = getDefault()
        if (current !== this) {
            fallback = current
            setDefault(this)
        }
    }

    override fun getPasswordAuthentication(): PasswordAuthentication? {
        val proxy = ProxyManager.currentProxy
        val credentials = proxy?.credentials
        if (credentials != null && requestingHost == proxy.host && requestingPort == proxy.port) {
            return PasswordAuthentication(credentials.username, credentials.password.toCharArray())
        }

        return fallback?.requestPasswordAuthenticationInstance(
            requestingHost, requestingSite, requestingPort, requestingProtocol,
            requestingPrompt, requestingScheme, requestingURL, requestorType
        )
    }

}
