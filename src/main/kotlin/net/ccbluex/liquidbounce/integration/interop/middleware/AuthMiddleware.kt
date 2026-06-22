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

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.cookie.DefaultCookie
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import io.netty.handler.codec.http.cookie.ServerCookieEncoder
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer.AUTH_CODE
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.netty.http.middleware.Middleware
import net.ccbluex.netty.http.model.RequestContext
import net.ccbluex.netty.http.util.httpUnauthorized

class AuthMiddleware : Middleware.OnRequest, Middleware.OnResponse,
    Middleware.OnWebSocketUpgrade {

    companion object {

        const val AUTH_COOKIE_NAME = "lb_auth"
        const val AUTH_CODE_PARAM = "lb_code"

        private fun isAuthenticated(headers: HttpHeaders): Boolean {
            val cookieHeader = headers[HttpHeaderNames.COOKIE] ?: return false

            val cookies = ServerCookieDecoder.STRICT.decode(cookieHeader)
            val authCookie = cookies.firstOrNull { it.name() == AUTH_COOKIE_NAME }

            return authCookie?.value() == AUTH_CODE
        }
    }

    /**
     * On request handler
     */
    override fun invoke(context: RequestContext): FullHttpResponse? {
        val codeParam = context.params[AUTH_CODE_PARAM]

        // Check if the authentication code is valid or if the request is already authenticated.
        if (codeParam != null && codeParam == AUTH_CODE || isAuthenticated(context.headers) ||
            ThemeManager.isThemeExternal) {
            // Allow the request to proceed.
            return null
        }

        logger.warn("[Interop] Unauthenticated request to ${context.httpMethod} ${context.path}")
        return httpUnauthorized("Authentication required")
    }

    /**
     * On response handler
     */
    override fun invoke(
        context: RequestContext,
        response: FullHttpResponse
    ): FullHttpResponse {
        val codeParam = context.params[AUTH_CODE_PARAM]
        if (codeParam != null) {
            // This should not happen, but we check it nevertheless.
            if (codeParam != AUTH_CODE) {
                return httpUnauthorized("The authentication code is invalid")
            }

            // This cookie allows the client to authenticate with the server
            // without providing the authentication code again.
            val cookie = DefaultCookie(AUTH_COOKIE_NAME, AUTH_CODE).apply {
                setPath("/")
                isHttpOnly = true
            }
            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie))
        }

        return response
    }

    /**
     * On web socket upgrade handler
     */
    override fun invoke(
        ctx: ChannelHandlerContext,
        request: HttpRequest
    ): FullHttpResponse? {
        if (!isAuthenticated(request.headers()) && !ThemeManager.isThemeExternal) {
            logger.warn("[Interop] Unauthenticated web socket upgrade request")
            return httpUnauthorized("Authentication required")
        }

        // Allow upgrade of the web socket connection
        return null
    }

}
