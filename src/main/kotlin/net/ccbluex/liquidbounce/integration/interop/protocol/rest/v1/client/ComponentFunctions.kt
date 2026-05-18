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

import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager
import net.ccbluex.netty.http.routing.Routing

// GET /api/v1/client/components
private fun Routing.getCurrentComponents() = get {
    call.respond(
        HudComponentManager.getComponents(null),
        accessibleInteropGson,
    )
}

// GET /api/v1/client/components/:id
private fun Routing.getComponents() = get("/:id") {
    call.respond(
        HudComponentManager.getComponents(call.parameters["id"]),
        accessibleInteropGson,
    )
}

internal fun Routing.componentRoutes() = route("/components") {
    getCurrentComponents()
    getComponents()
}
