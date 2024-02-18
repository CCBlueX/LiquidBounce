/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpBadRequest
import net.ccbluex.liquidbounce.web.socket.netty.httpFileStream
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.util.Identifier

fun RestNode.resourceRest() {
    get("/resource") { request ->
        val identifier = request.params["id"]
            ?: return@get httpBadRequest("Missing identifier parameter")
        val minecraftIdentifier = Identifier(identifier)
        val resource = mc.resourceManager.getResourceOrThrow(minecraftIdentifier)

        resource.inputStream.use {
            httpFileStream(it)
        }
    }
}
