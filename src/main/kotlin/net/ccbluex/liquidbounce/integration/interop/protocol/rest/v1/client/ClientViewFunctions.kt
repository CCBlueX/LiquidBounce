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

import io.ktor.http.ContentType
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeStringUtf8
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.ccbluex.liquidbounce.features.client.view.ClientViews
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.notFound

private const val BOUNDARY = "frame"
private val MJPEG = ContentType.parse("multipart/x-mixed-replace; boundary=$BOUNDARY")

// GET /api/v1/client/clients/{id}/view
private fun Route.getClientView() = get("/clients/{id}/view") {
    val id = call.parameters["id"]?.toIntOrNull() ?: call.badRequest("Missing client id")
    val client = ClientManager.byId(id)?.takeUnless { it.isActive }
        ?: call.notFound("/clients/$id/view", "No background client $id")

    call.respondBytesWriter(MJPEG) {
        ClientViews.watch(client) { frames ->
            frames.collect { jpeg ->
                writeStringUtf8("--$BOUNDARY\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\n\r\n")
                writeByteArray(jpeg)
                writeStringUtf8("\r\n")
                flush()
            }
        }
    }
}

internal fun Route.clientViewRoutes() {
    getClientView()
}
