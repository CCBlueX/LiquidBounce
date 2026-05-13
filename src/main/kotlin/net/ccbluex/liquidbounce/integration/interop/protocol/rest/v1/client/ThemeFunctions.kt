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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.netty.http.routing.RoutingContext

// GET /api/v1/client/theme/:id
fun RoutingContext.getTheme() {
    val id = parameters["id"]
    val theme = if (id != null) {
        ThemeManager.themes.find { it.metadata.id == id } ?: notFound(id, "Theme not found")
    } else {
        ThemeManager.theme
    }

    respond(accessibleInteropGson.toJsonTree(theme))
}

// GET /api/v1/client/shader
fun RoutingContext.getToggleShaderInfo() {
    respond(JsonObject().apply {
        addProperty("shaderEnabled", ThemeManager.shaderEnabled)
    })
}

// POST /api/v1/client/shader
fun RoutingContext.postToggleShader() {
    ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
    ConfigSystem.store(ThemeManager)
    respondNoContent()
}


// GET /api/v1/client/fonts
fun RoutingContext.getFonts() {
    respond(FontManager.fontFaces.keys, interopGson)
}

// GET /api/v1/client/fonts/:name
fun RoutingContext.getFont() {
    val name = parameters["name"] ?: badRequest("Missing font name")
    val font = FontManager.fontFace(name) ?: notFound(name, "Font not found")
    val file = font.file ?: run {
        respondNoContent()
        return
    }

    respondFile(file)
}
