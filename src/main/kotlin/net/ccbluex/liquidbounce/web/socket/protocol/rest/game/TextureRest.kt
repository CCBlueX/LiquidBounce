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

import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpBadRequest
import net.ccbluex.liquidbounce.web.socket.netty.httpFileStream
import net.ccbluex.liquidbounce.web.socket.netty.httpInternalServerError
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun RestNode.resourceRest() {
    get("/resource") { request ->
        val identifier = request.params["id"]
            ?: return@get httpBadRequest("Missing identifier parameter")
        val minecraftIdentifier = Identifier(identifier)
        val resource = mc.resourceManager.getResourceOrThrow(minecraftIdentifier)

        resource.inputStream.use {
            httpFileStream(it)
        }
    }.apply {
        get("/itemTexture") { request ->
            if (!ItemImageAtlas.isAtlasAvailable) {
                return@get httpInternalServerError("Item atlas not available yet")
            }

            val identifier = request.params["id"]
                ?: return@get httpBadRequest("Missing identifier parameter")
            val minecraftIdentifier = runCatching { Identifier(identifier) }.getOrNull()
                ?: return@get httpBadRequest("Invalid identifier")

            val of = RegistryKey.of(RegistryKeys.ITEM, minecraftIdentifier)

            val resource = Registries.ITEM.get(of)
                ?: return@get httpBadRequest("Item not found")

            val writer = ByteArrayOutputStream(2048)

            ImageIO.write(ItemImageAtlas.getItemImage(resource), "PNG", writer)

            httpFileStream(ByteArrayInputStream(writer.toByteArray()))
//            httpFileStream(resource)
        }
    }
}
