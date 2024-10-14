/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.web.socket.netty

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.socket.netty.model.RequestObject
import net.ccbluex.liquidbounce.web.socket.netty.rest.RouteController

class HttpConductor {

    fun processRequestObject(requestObject: RequestObject) = runCatching {
        val context = requestObject.context
        val method = context.httpMethod

        logger.debug("Request {}", requestObject)

        if (!context.headers["content-length"].isNullOrEmpty() &&
            context.headers["content-length"]?.toInt() != requestObject.content.toByteArray(Charsets.UTF_8).size) {
            logger.warn("Received incomplete request: $requestObject")
            return@runCatching httpBadRequest("Incomplete request")
        }

        if (method == HttpMethod.OPTIONS) {
            val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(ByteArray(0))
            )

            val httpHeaders = response.headers()
            httpHeaders[HttpHeaderNames.CONTENT_TYPE] = "text/plain"
            httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()

            val requestOrigin = context.headers["origin"]
            if (requestOrigin == "http://localhost" || requestOrigin == "http://127.0.0.1") {
                httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = requestOrigin
            }

            httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS] = "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS"
            httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS] = "Content-Type, Content-Length, Authorization, Accept, X-Requested-With"
            return@runCatching response
        }

        RouteController.findRoute(context.path, method)?.let { route ->
            logger.debug("Found route {}", route)
            return@runCatching route.handler(requestObject)
        }

        if (method == HttpMethod.GET) {
            RouteController.findFileServant(context.path)?.let { (fileServant, path) ->
                logger.debug("Found file servant {}", fileServant)
                return@runCatching fileServant.handleFileRequest(path)
            }
        }

        httpNotFound(context.path, "Route not found")
    }.getOrElse {
        logger.error("Error while processing request object: $requestObject", it)
        httpInternalServerError(it.message ?: "Unknown error")
    }


}
