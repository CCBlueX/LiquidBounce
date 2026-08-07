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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1

import com.google.gson.stream.JsonWriter
import com.mojang.blaze3d.platform.NativeImage
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondTextWriter
import net.minecraft.server.packs.resources.Resource
import java.awt.image.BufferedImage
import java.nio.channels.Channels
import javax.imageio.ImageIO

suspend fun ApplicationCall.respondResource(
    resource: Resource,
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
) = respondOutputStream(contentType, status) {
    resource.open().use {
        it.transferTo(this)
    }
}

suspend fun ApplicationCall.respondImage(
    image: NativeImage,
    contentType: ContentType? = ContentType.Image.PNG,
    status: HttpStatusCode? = null,
) = respondOutputStream(contentType, status) {
    image.writeToChannel(Channels.newChannel(this))
}

suspend fun ApplicationCall.respondImage(
    image: BufferedImage,
    contentType: ContentType? = ContentType.Image.PNG,
    status: HttpStatusCode? = null,
) = respondOutputStream(contentType, status) {
    ImageIO.write(image, "PNG", this)
}

suspend fun ApplicationCall.respondJsonWriter(
    contentType: ContentType? = ContentType.Application.Json,
    status: HttpStatusCode? = null,
    block: suspend JsonWriter.() -> Unit,
) = respondTextWriter(contentType, status) {
    JsonWriter(this).use {
        it.block()
    }
}
