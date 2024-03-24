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
package net.ccbluex.liquidbounce.web.socket.protocol.rest.client

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.theme.ThemeManager

internal fun RestNode.themeRest() {
    get("/theme") {
        httpOk(JsonObject().apply {
            addProperty("activeTheme", ThemeManager.activeTheme.name)
            addProperty("shaderEnabled", ThemeManager.shaderEnabled)
        })
    }.apply {
        post("/shader") {
            data class ShaderState(val enabled: Boolean)
            val shaderState = decode<ShaderState>(it.content)

            ThemeManager.shaderEnabled = shaderState.enabled
            ConfigSystem.storeConfigurable(ThemeManager)
            httpOk(JsonObject())
        }.apply {
            post("/switch") {
                ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
                ConfigSystem.storeConfigurable(ThemeManager)
                httpOk(JsonObject())
            }
        }

    }
}
