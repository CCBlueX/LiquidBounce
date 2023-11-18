/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.web.socket.netty.rest

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import net.ccbluex.liquidbounce.web.socket.netty.model.RequestObject

object RouteController {

    private val paths = mutableListOf<RestNode>()

    fun new(path: String): RestNode {
        val restNode = RestNode(path)
        paths += restNode
        return restNode
    }

    /**
     * Find a route for the given URI path and http method
     *
     * @param path URI path
     * @param method HTTP method
     * @return Route or null if no route was found
     *
     * @example findRoute("/api/v1/users", HttpMethod.GET)
     */
    fun findRoute(path: String, method: HttpMethod): Route? {
        val pathNode = paths.find { path.startsWith(it.path) } ?: return null
        val lastPath = path.substring(pathNode.path.length)
        return pathNode.routes.find { it.name == lastPath && it.method == method }
    }

}

data class RestNode(val path: String) {

    internal val routes = mutableListOf<Route>()

    fun get(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.GET, handler)
    }

    fun post(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.POST, handler)
    }

    fun put(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.PUT, handler)
    }

    fun delete(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.DELETE, handler)
    }

    fun patch(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.PATCH, handler)
    }

    fun head(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.HEAD, handler)
    }

    fun options(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.OPTIONS, handler)
    }

    fun trace(path: String, handler: (RequestObject) -> FullHttpResponse) {
        routes += Route(path, HttpMethod.TRACE, handler)
    }

}

data class Route(val name: String, val method: HttpMethod, val handler: (RequestObject) -> FullHttpResponse)
