/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.interop.protocol.rest.v1.client

import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import net.ccbluex.netty.http.util.httpBadRequest

// GET /api/v1/client/theme
@Suppress("UNUSED_PARAMETER")
fun getThemeInfo(requestObject: RequestObject): FullHttpResponse = httpOk(JsonObject().apply {
    addProperty("activeTheme", ThemeManager.activeTheme.name)
    addProperty("shaderEnabled", ThemeManager.shaderEnabled)
})

// POST /api/v1/client/theme/shader
@Suppress("UNUSED_PARAMETER")
fun postShaderState(requestObject: RequestObject): FullHttpResponse {
    data class ShaderState(val enabled: Boolean)
    val shaderState = requestObject.asJson<ShaderState>() ?: return httpBadRequest("Invalid request data")

    ThemeManager.shaderEnabled = shaderState.enabled
    ConfigSystem.storeConfigurable(ThemeManager)
    return httpOk(JsonObject())
}

// POST /api/v1/client/theme/switch
@Suppress("UNUSED_PARAMETER")
fun postThemeSwitch(requestObject: RequestObject): FullHttpResponse {
    ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
    ConfigSystem.storeConfigurable(ThemeManager)
    return httpOk(JsonObject())
}
