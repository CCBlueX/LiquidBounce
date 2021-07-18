/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.render.ultralight.js.bindings

import com.thealtening.api.TheAltening
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.loginAltening
import net.ccbluex.liquidbounce.utils.client.loginCracked
import net.ccbluex.liquidbounce.utils.client.loginMojang
import net.ccbluex.liquidbounce.utils.client.mc
import java.net.InetSocketAddress

/**
 * Referenced by JS as `client`
 */
object UltralightJsClient {

    val moduleManager = ModuleManager
    val sessionService = UltralightJsSessionService
    val proxyManager = UltralightJsProxyManager
    val theAltening = UltralightAlteningService

    /**
     * Access session service from Ultralight
     */
    object UltralightJsSessionService {

        fun loginCracked(username: String) = mc.sessionService.loginCracked(username).readable
        fun loginMojang(email: String, password: String) = mc.sessionService.loginMojang(email, password).readable
        fun loginAltening(token: String) = mc.sessionService.loginAltening(token).readable

        fun getFaceUrl() = "https://visage.surgeplay.com/face/${mc.session.uuid}"
        fun getUsername(): String = mc.session.username

    }

    /**
     * Access Proxy Manager from Ultralight
     */
    object UltralightJsProxyManager {

        fun setProxy(host: String, port: Int, username: String, password: String): String {
            ProxyManager.currentProxy = ProxyManager.Proxy(InetSocketAddress(host, port), if (username.isNotBlank()) ProxyManager.ProxyCredentials(username, password) else null)

            return "Successfully set proxy"
        }

        fun unsetProxy(): String {
            ProxyManager.currentProxy = null
            return "Successfully unset proxy"
        }

    }

    /**
     * Access Altening API from Ultralight
     */
    object UltralightAlteningService {

        fun generateAccount(token: String) = TheAltening.newBasicRetriever(token).account.token

    }

}
