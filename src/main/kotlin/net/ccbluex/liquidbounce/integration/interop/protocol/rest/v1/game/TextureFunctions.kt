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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpFileStream
import net.ccbluex.netty.http.util.httpInternalServerError
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import java.util.*
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrNull

// GET /api/v1/client/resource
@Suppress("UNUSED_PARAMETER")
fun getResource(requestObject: RequestObject) = run {
    val identifier = requestObject.queryParams["id"]
        ?: return@run httpBadRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.of(identifier)
    val resource = mc.resourceManager.getResourceOrThrow(minecraftIdentifier)

    resource.inputStream.use {
        httpFileStream(it)
    }
}

// GET /api/v1/client/itemTexture
@Suppress("UNUSED_PARAMETER")
fun getItemTexture(requestObject: RequestObject) = run {
    if (!ItemImageAtlas.isAtlasAvailable) {
        return@run httpInternalServerError("Item atlas not available yet")
    }

    val identifier = requestObject.queryParams["id"]
        ?: return@run httpBadRequest("Missing identifier parameter")
    val minecraftIdentifier = runCatching { Identifier.of(identifier) }.getOrNull()
        ?: return@run httpBadRequest("Invalid identifier")

    val alternativeIdentifier = ItemImageAtlas.resolveAliasIfPresent(minecraftIdentifier)

    val of = RegistryKey.of(RegistryKeys.ITEM, alternativeIdentifier)

    val image = Registries.ITEM.get(of)?.let(ItemImageAtlas::getItemImage)
        ?: return@run httpBadRequest("Item image not found")

    val buffer = okio.Buffer()
    ImageIO.write(image, "PNG", buffer.outputStream())
    httpFileStream(buffer.inputStream())
}

// GET /api/v1/client/skin
fun getSkin(requestObject: RequestObject) = run {
    val uuid = requestObject.queryParams["uuid"]?.let { UUID.fromString(it) }
        ?: return@run httpBadRequest("Missing UUID parameter")
    val skinTextures = world.players.find { it.uuid == uuid }?.skinTextures
        ?: DefaultSkinHelper.getSkinTextures(uuid)
    val texture = mc.textureManager.getTexture(skinTextures.texture)

    if (texture is NativeImageBackedTexture) {
        val buffer = okio.Buffer()
        texture.image?.write(buffer) ?: return@run httpInternalServerError("Texture is not cached yet")
        httpFileStream(buffer.inputStream())
    } else {
        val resource = mc.resourceManager.getResource(skinTextures.texture)
            .getOrNull() ?: return@run httpInternalServerError("Texture not found")

        resource.inputStream.use {
            httpFileStream(it)
        }
    }
}
