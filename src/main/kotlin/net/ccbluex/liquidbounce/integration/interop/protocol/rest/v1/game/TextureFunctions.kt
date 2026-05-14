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

import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.respondInputStream
import net.ccbluex.liquidbounce.render.gui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.netty.http.routing.Routing
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import java.nio.channels.Channels
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrNull

// GET /api/v1/client/resource
private fun Routing.getResource() = get {
    val identifier = call.queryParameters["id"]
        ?: call.badRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: call.badRequest("Invalid identifier $identifier")
    val resource = mc.resourceManager.getResourceOrThrow(minecraftIdentifier)

    call.respondInputStream(resource.open(), contentType = "image/png")
}

// GET /api/v1/client/itemTexture
private fun Routing.getItemTexture() = get("/itemTexture") {
    if (!ItemImageAtlas.isAtlasAvailable) {
        call.internalServerError("Item atlas not available yet")
    }

    val identifier = call.queryParameters["id"]
        ?: call.badRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: call.badRequest("Invalid identifier $identifier")

    val alternativeIdentifier = ItemImageAtlas.resolveAliasIfPresent(minecraftIdentifier)

    val of = ResourceKey.create(Registries.ITEM, alternativeIdentifier)

    val image = BuiltInRegistries.ITEM.getValue(of)?.let(ItemImageAtlas::getItemImage)
        ?: call.badRequest("Item image not found")

    call.respondOutputStream(contentType = "image/png") {
        ImageIO.write(image, "PNG", this)
    }
}

// GET /api/v1/client/effectTexture
private fun Routing.getEffectTexture() = get("/effectTexture") {
    val identifier = call.queryParameters["id"]
        ?: call.badRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: call.badRequest("Invalid identifier $identifier")

    val textureId = Identifier.withDefaultNamespace("textures/mob_effect/${minecraftIdentifier.path}.png")

    val resource = mc.resourceManager.getResource(textureId).getOrNull()
        ?: call.badRequest("Mob effect texture of $minecraftIdentifier not found")

    call.respondInputStream(resource.open(), contentType = "image/png")
}

// GET /api/v1/client/skin
private fun Routing.getSkin() = get("/skin") {
    val uuid = call.queryParameters["uuid"]?.let { UUID.fromString(it) }
        ?: call.badRequest("Missing UUID parameter")
    val skinTextures = world.players().find { it.uuid == uuid }?.skin
        ?: DefaultPlayerSkin.get(uuid)
    val bodyTexturePath = skinTextures.body.texturePath()
    val texture = mc.textureManager.getTexture(bodyTexturePath)

    if (texture is DynamicTexture) {
        val nativeImage = texture.pixels ?: call.internalServerError("Texture is not cached yet")
        call.respondOutputStream(contentType = "image/png") {
            Channels.newChannel(this).use(nativeImage::writeToChannel)
        }
    } else {
        val resource = mc.resourceManager.getResource(bodyTexturePath)
            .getOrNull() ?: call.internalServerError("Texture not found")

        call.respondInputStream(resource.open(), contentType = "image/png")
    }
}

internal fun Routing.textureRoutes() = route("/resource") {
    getResource()
    getItemTexture()
    getEffectTexture()
    getSkin()
}
