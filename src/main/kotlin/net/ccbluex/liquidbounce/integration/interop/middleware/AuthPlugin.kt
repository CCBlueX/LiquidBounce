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
package net.ccbluex.liquidbounce.integration.interop.middleware

import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.websocket.WebSocketServerSession
import net.ccbluex.liquidbounce.integration.interop.unauthorized
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.logger

object AuthConfig {
    const val AUTH_COOKIE_NAME = "lb_auth"
    const val AUTH_CODE_PARAM = "lb_code"
}

class AuthPluginConfig {
    var authCode: String? = null
}

val AuthPlugin = createApplicationPlugin(name = "AuthPlugin", createConfiguration = ::AuthPluginConfig) {

    onCall { call ->
        val code = pluginConfig.authCode ?: return@onCall

        val codeParam = call.request.queryParameters[AuthConfig.AUTH_CODE_PARAM]

        if (codeParam != null && codeParam == code || isAuthenticated(call, code) || ThemeManager.isThemeExternal) {
            return@onCall
        }

        logger.warn("[Interop] Unauthenticated request to ${call.request.local.method.value} ${call.request.local.uri}")
        call.unauthorized("Authentication required")
    }

    onCallRespond { call, _ ->
        val code = pluginConfig.authCode ?: return@onCallRespond

        val codeParam = call.request.queryParameters[AuthConfig.AUTH_CODE_PARAM]
        if (codeParam != null && codeParam == code) {
            call.response.cookies.append(
                Cookie(AuthConfig.AUTH_COOKIE_NAME, code, httpOnly = true, path = "/")
            )
        }
    }
}

private fun isAuthenticated(call: ApplicationCall, authCode: String): Boolean {
    return call.request.cookies[AuthConfig.AUTH_COOKIE_NAME] == authCode
}

fun isWebSocketAuthenticated(session: WebSocketServerSession, authCode: String): Boolean {
    return isAuthenticated(session.call, authCode)
}
