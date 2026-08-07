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
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.notFound
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.render.FontManager

// GET /api/v1/client/theme
private fun Route.getCurrentTheme() = get {
    call.respond(accessibleInteropGson.toJsonTree(ThemeManager.theme))
}

// GET /api/v1/client/theme/{id}
private fun Route.getTheme() = get("/{id}") {
    val id = call.parameters["id"] ?: call.forbidden("No id")
    val theme = ThemeManager.themes.find { it.metadata.id == id } ?: call.notFound(id, "Theme not found")

    call.respond(accessibleInteropGson.toJsonTree(theme))
}

// GET /api/v1/client/shader
private fun Route.getToggleShaderInfo() = get {
    call.respond(JsonObject().apply {
        addProperty("shaderEnabled", ThemeManager.shaderEnabled)
    })
}

// POST /api/v1/client/shader
private fun Route.postToggleShader() = post {
    ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
    ConfigSystem.store(ThemeManager)
    call.respond(HttpStatusCode.NoContent)
}


// GET /api/v1/client/fonts
private fun Route.getFonts() = get { call.respond(FontManager.fontFaces.keys) }

// GET /api/v1/client/fonts/{name}
private fun Route.getFont() = get("/{name}") {
    val name = call.parameters["name"] ?: call.badRequest("Missing font name")
    val font = FontManager.fontFace(name) ?: call.notFound(name, "Font not found")
    val file = font.file ?: run {
        call.respond(io.ktor.http.HttpStatusCode.NoContent)
        return@get
    }

    call.respondFile(file)
}

internal fun Route.themeRoutes() {
    route("/theme") {
        getCurrentTheme()
        getTheme()
    }
    route("/shader") {
        getToggleShaderInfo()
        postToggleShader()
    }
}
