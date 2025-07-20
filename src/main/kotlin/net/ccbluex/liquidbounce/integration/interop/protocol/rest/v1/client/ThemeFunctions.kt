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
 */
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.*

// GET /api/v1/client/theme
@Suppress("UNUSED_PARAMETER")
fun getThemeInfo(requestObject: RequestObject): FullHttpResponse = httpOk(JsonObject().apply {
    addProperty("activeTheme", ThemeManager.activeTheme.name)
    addProperty("shaderEnabled", ThemeManager.shaderEnabled)
})

// POST /api/v1/client/shader
@Suppress("UNUSED_PARAMETER")
fun postToggleShader(requestObject: RequestObject): FullHttpResponse {
    ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
    ConfigSystem.storeConfigurable(ThemeManager)
    return httpOk(emptyJsonObject())
}


// GET /api/v1/client/fonts
@Suppress("UNUSED_PARAMETER")
fun getFonts(requestObject: RequestObject): FullHttpResponse = httpOk(JsonArray().apply {
    FontManager.fontFaces.forEach { (name, _) ->
        add(name)
    }
})

// GET /api/v1/client/fonts/:name
@Suppress("UNUSED_PARAMETER")
fun getFont(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"] ?: return httpBadRequest("Missing font name")
    val font = FontManager.fontFace(name) ?: return httpNotFound(name, "Font not found")
    val file = font.file ?: return httpNoContent()

    return httpFile(file)
}
