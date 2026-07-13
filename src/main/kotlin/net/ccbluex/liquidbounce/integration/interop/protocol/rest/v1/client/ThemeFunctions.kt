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
import net.ccbluex.netty.http.routing.Routing

// GET /api/v1/client/theme
private fun Routing.getCurrentTheme() = get {
    call.respond(accessibleInteropGson.toJsonTree(ThemeManager.theme))
}

// GET /api/v1/client/theme/:id
private fun Routing.getTheme() = get("/:id") {
    val id = call.parameters["id"] ?: call.forbidden("No id")
    val theme = ThemeManager.themes.find { it.metadata.id == id } ?: call.notFound(id, "Theme not found")

    call.respond(accessibleInteropGson.toJsonTree(theme))
}

// GET /api/v1/client/shader
private fun Routing.getToggleShaderInfo() = get {
    call.respond(JsonObject().apply {
        addProperty("shaderEnabled", ThemeManager.shaderEnabled)
    })
}

// POST /api/v1/client/shader
private fun Routing.postToggleShader() = post {
    ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
    ConfigSystem.store(ThemeManager)
    call.respondNoContent()
}


// GET /api/v1/client/fonts
private fun Routing.getFonts() = get { call.respond(FontManager.fontFaces.keys, interopGson) }

// GET /api/v1/client/fonts/:name
private fun Routing.getFont() = get("/:name") {
    val name = call.parameters["name"] ?: call.badRequest("Missing font name")
    val font = FontManager.fontFace(name) ?: call.notFound(name, "Font not found")
    val file = font.file ?: run {
        call.respondNoContent()
        return@get
    }

    call.respondFile(file)
}

internal fun Routing.themeRoutes() {
    route("/theme") {
        getCurrentTheme()
        getTheme()
    }
    route("/shader") {
        getToggleShaderInfo()
        postToggleShader()
    }
}
