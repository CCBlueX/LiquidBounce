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
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.socket.netty.model.RequestObject

object RouteController : RestNode("")

open class RestNode(val path: String) {

    internal val nodes = mutableListOf<RestNode>()

    fun new(path: String) = RestNode(path).also { nodes += it }

    fun get(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.GET, handler).also { nodes += it }

    fun post(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.POST, handler).also { nodes += it }

    fun put(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.PUT, handler).also { nodes += it }

    fun delete(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.DELETE, handler).also { nodes += it }

    fun patch(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.PATCH, handler).also { nodes += it }

    fun head(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.HEAD, handler).also { nodes += it }

    fun options(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.OPTIONS, handler).also { nodes += it }

    fun trace(path: String, handler: (RequestObject) -> FullHttpResponse)
        = Route(path, HttpMethod.TRACE, handler).also { nodes += it }

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
        logger.debug("--------- ${if (this is Route) "ROUTE" else "NODE"} ${this.path} ---------")

        val takenOff = path.substring(this.path.length)
        logger.debug("Search for route: {} {}", method, takenOff)

        // Nodes can now either include a route with the correct method or a node with a path that matches
        // the given path
        val nodes = nodes.filter {
            logger.debug("Check node: $takenOff startsWith ${it.path} -> ${path.startsWith(it.path)}")
            takenOff.startsWith(it.path)
        }

        logger.debug("Found ${nodes.size} nodes")

        // Now we have to decide if the route matches the path exactly or if it is a node step
        val exactMatch = nodes.filterIsInstance<Route>().find {
            logger.debug("Check route: {} == {} && {} == {}", it.path, takenOff, it.method, method)
            it.method == method && it.path == takenOff
        }
        if (exactMatch != null) {
            return exactMatch
        }

        logger.debug("No exact match found")

        // If we have no exact match we have to find the node that matches the path
        val nodeMatch = nodes.firstOrNull() ?: return null
        logger.debug("Found node match: ${nodeMatch.path}")
        return nodeMatch.findRoute(takenOff, method)
    }

}

class Route(name: String, val method: HttpMethod, val handler: (RequestObject) -> FullHttpResponse)
    : RestNode(name)
