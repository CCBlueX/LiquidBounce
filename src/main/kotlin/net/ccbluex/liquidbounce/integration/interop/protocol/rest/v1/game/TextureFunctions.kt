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

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.internalServerError
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.respondImage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.respondResource
import net.ccbluex.liquidbounce.integration.interop.serviceUnavailable
import net.ccbluex.liquidbounce.render.gui.AtlasLookup
import net.ccbluex.liquidbounce.render.gui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.Identifier
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

// GET /api/v1/client/resource
private fun Route.getResource() = get {
    val identifier = call.queryParameters["id"]
        ?: call.badRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: call.badRequest("Invalid identifier $identifier")
    val resource = mc.resourceManager.getResourceOrThrow(minecraftIdentifier)

    call.respondResource(resource, ContentType.Image.PNG)
}

// GET /api/v1/client/resource/itemTexture
private fun Route.getItemTexture() = get("/itemTexture") {
    val identifier = call.queryParameters["id"]
        ?: call.badRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: call.badRequest("Invalid identifier $identifier")

    when (val result = ItemImageAtlas.getItemImage(minecraftIdentifier)) {
        is AtlasLookup.Found -> call.respondBytes(result.bytes, ContentType.Image.PNG)
        AtlasLookup.Missing -> call.badRequest("Item image not found")
        AtlasLookup.NotReady -> {
            call.response.header(HttpHeaders.RetryAfter, 5)
            call.serviceUnavailable("Item atlas not available yet")
        }
    }
}

// GET /api/v1/client/resource/effectTexture
private fun Route.getEffectTexture() = get("/effectTexture") {
    val identifier = call.queryParameters["id"]
        ?: call.badRequest("Missing identifier parameter")
    val minecraftIdentifier = Identifier.tryParse(identifier)
        ?: call.badRequest("Invalid identifier $identifier")

    val textureId = Identifier.withDefaultNamespace("textures/mob_effect/${minecraftIdentifier.path}.png")

    val resource = mc.resourceManager.getResource(textureId).getOrNull()
        ?: call.badRequest("Mob effect texture of $minecraftIdentifier not found")

    call.respondResource(resource, ContentType.Image.PNG)
}

// GET /api/v1/client/resource/skin
private fun Route.getSkin() = get("/skin") {
    val uuid = call.queryParameters["uuid"]?.let { UUID.fromString(it) }
        ?: call.badRequest("Missing UUID parameter")
    val skinTextures = world.players().find { it.uuid == uuid }?.skin
        ?: DefaultPlayerSkin.get(uuid)
    val bodyTexturePath = skinTextures.body.texturePath()
    val texture = mc.textureManager.getTexture(bodyTexturePath)

    if (texture is DynamicTexture) {
        val nativeImage = texture.pixels ?: call.internalServerError("Texture is not cached yet")
        call.respondImage(nativeImage)
    } else {
        val resource = mc.resourceManager.getResource(bodyTexturePath)
            .getOrNull() ?: call.internalServerError("Texture not found")

        call.respondResource(resource, ContentType.Image.PNG)
    }
}

internal fun Route.textureRoutes() = route("/resource") {
    getResource()
    getItemTexture()
    getEffectTexture()
    getSkin()
}
