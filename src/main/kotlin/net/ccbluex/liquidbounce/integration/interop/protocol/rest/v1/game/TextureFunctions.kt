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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpFileStream
import net.ccbluex.netty.http.util.httpInternalServerError
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrNull

// GET /api/v1/client/resource
@Suppress("UNUSED_PARAMETER")
fun getResource(requestObject: RequestObject) = run {
    val identifier = requestObject.queryParams["id"]
        ?: return@run httpBadRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: return@run httpBadRequest("Invalid identifier $identifier")
    val resource = mc.resourceManager.getResourceOrThrow(minecraftIdentifier)

    return httpFileStream(resource.open(), contentType = "image/png")
}

// GET /api/v1/client/itemTexture
@Suppress("UNUSED_PARAMETER")
fun getItemTexture(requestObject: RequestObject) = run {
    if (!ItemImageAtlas.isAtlasAvailable) {
        return@run httpInternalServerError("Item atlas not available yet")
    }

    val identifier = requestObject.queryParams["id"]
        ?: return@run httpBadRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: return@run httpBadRequest("Invalid identifier $identifier")

    val alternativeIdentifier = ItemImageAtlas.resolveAliasIfPresent(minecraftIdentifier)

    val of = ResourceKey.create(Registries.ITEM, alternativeIdentifier)

    val image = BuiltInRegistries.ITEM.getValue(of)?.let(ItemImageAtlas::getItemImage)
        ?: return@run httpBadRequest("Item image not found")

    val buffer = okio.Buffer()
    ImageIO.write(image, "PNG", buffer.outputStream())
    httpFileStream(buffer.inputStream(), contentLength = buffer.size.toInt(), contentType = "image/png")
}

// GET /api/v1/client/effectTexture
@Suppress("UNUSED_PARAMETER")
fun getEffectTexture(requestObject: RequestObject): FullHttpResponse {
    val identifier = requestObject.queryParams["id"]
        ?: return httpBadRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: return httpBadRequest("Invalid identifier $identifier")

    val textureId = Identifier.withDefaultNamespace("textures/mob_effect/${minecraftIdentifier.path}.png")

    val resource = mc.resourceManager.getResource(textureId).getOrNull()
        ?: return httpBadRequest("Mob effect texture of $minecraftIdentifier not found")

    return httpFileStream(resource.open(), contentType = "image/png")
}

// GET /api/v1/client/skin
fun getSkin(requestObject: RequestObject) = run {
    val uuid = requestObject.queryParams["uuid"]?.let { UUID.fromString(it) }
        ?: return@run httpBadRequest("Missing UUID parameter")
    val skinTextures = world.players().find { it.uuid == uuid }?.skin
        ?: DefaultPlayerSkin.get(uuid)
    val bodyTexturePath = skinTextures.body.texturePath()
    val texture = mc.textureManager.getTexture(bodyTexturePath)

    if (texture is DynamicTexture) {
        val buffer = okio.Buffer()
        texture.pixels?.writeToChannel(buffer) ?: return@run httpInternalServerError("Texture is not cached yet")
        httpFileStream(buffer.inputStream(), contentLength = buffer.size.toInt(), contentType = "image/png")
    } else {
        val resource = mc.resourceManager.getResource(bodyTexturePath)
            .getOrNull() ?: return@run httpInternalServerError("Texture not found")

        httpFileStream(resource.open(), contentType = "image/png")
    }
}
