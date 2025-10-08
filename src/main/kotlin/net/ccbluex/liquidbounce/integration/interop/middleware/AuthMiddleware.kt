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

package net.ccbluex.liquidbounce.integration.interop.middleware

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.cookie.DefaultCookie
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import io.netty.handler.codec.http.cookie.ServerCookieEncoder
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.netty.http.middleware.Middleware
import net.ccbluex.netty.http.model.RequestContext
import net.ccbluex.netty.http.util.httpUnauthorized
import okhttp3.Headers.Companion.toHeaders
import org.apache.commons.lang3.RandomStringUtils

class AuthMiddleware : Middleware {

    companion object {

        val AUTH_CODE: String = RandomStringUtils.secure().nextAlphanumeric(16)
        const val AUTH_COOKIE_NAME = "lb_auth"
        const val AUTH_CODE_PARAM = "lb_code"

        private val PUBLIC_PATHS = emptySet<String>()

        private fun isAuthenticated(context: RequestContext): Boolean {
            val cookieHeader = context.headers.toHeaders()[HttpHeaderNames.COOKIE.toString()] ?: return false

            val cookies = ServerCookieDecoder.STRICT.decode(cookieHeader)
            val authCookie = cookies.firstOrNull { it.name() == AUTH_COOKIE_NAME }

            return authCookie?.value() == AUTH_CODE
        }
    }

    override fun invoke(
        context: RequestContext,
        response: FullHttpResponse
    ): FullHttpResponse {
        val path = context.path

        val initCode = context.params[AUTH_CODE_PARAM]
        if (initCode != null) {
            if (initCode != AUTH_CODE) {
                return httpUnauthorized("Invalid authentication code")
            }

            val cookie = DefaultCookie(AUTH_COOKIE_NAME, AUTH_CODE).apply {
                setPath("/")
                isHttpOnly = true
            }

            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie))

            return response
        }

        if (!PUBLIC_PATHS.contains(path) && !isAuthenticated(context)
            && !ThemeManager.theme.origin.external) {
            return httpUnauthorized("Code required")
        }
        return response
    }

}
