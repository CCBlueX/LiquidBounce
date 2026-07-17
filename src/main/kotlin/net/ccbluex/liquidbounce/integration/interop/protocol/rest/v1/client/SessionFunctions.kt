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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.utils.client.mc

// GET /api/v1/client/session
private fun Route.getSessionInfo() = get("/session") { call.respond(interopGson.toJsonTree(mc.user)) }

// GET /api/v1/client/location
private fun Route.getLocationInfo() = get("/location") {
    val locationInfo = IpInfoApi.current ?: call.forbidden("Location is not known")
    call.respond(interopGson.toJsonTree(locationInfo))
}

internal fun Route.sessionRoutes() {
    getSessionInfo()
    getLocationInfo()
}
