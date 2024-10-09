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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import net.ccbluex.netty.http.util.httpBadRequest

// GET /api/v1/client/wallpaper
@Suppress("UNUSED_PARAMETER")
fun getWallpaper(requestObject: RequestObject): FullHttpResponse =
    httpOk(JsonObject().apply {
        add("active", JsonObject().apply {
            val activeWallpaper = ThemeManager.activeWallpaper ?: return@apply

            addProperty("name", activeWallpaper.name)
            addProperty("theme", activeWallpaper.theme.name)
        })
        add("available", JsonArray().apply {
            ThemeManager.availableThemes.forEach { theme ->
                theme.wallpapers.forEach { wallpaper ->
                    add(JsonObject().apply {
                        addProperty("name", wallpaper.name)
                        addProperty("theme", theme.name)
                    })
                }
            }
        })
    })

// PUT /api/v1/client/wallpaper/:theme/:name
fun putWallpaper(requestObject: RequestObject): FullHttpResponse {
    val theme = requestObject.params["theme"] ?: return httpBadRequest("Missing theme")
    val name = requestObject.params["name"] ?: return httpBadRequest("Missing name")

    val wallpaper = ThemeManager.availableThemes
        .firstOrNull { it.name == theme }
        ?.wallpapers
        ?.firstOrNull { it.name == name }
        ?: return httpBadRequest("Invalid theme or wallpaper")

    ThemeManager.activeWallpaper = wallpaper
    RenderSystem.recordRenderCall(wallpaper::load)

    return httpOk(JsonObject())
}
