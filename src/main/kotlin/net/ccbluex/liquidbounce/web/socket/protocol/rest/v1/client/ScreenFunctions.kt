/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest.v1.client

import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.client.gui.screen.SplashOverlay

// GET /api/v1/client/virtualScreen
@Suppress("UNUSED_PARAMETER")
fun getVirtualScreenInfo(requestObject: RequestObject): FullHttpResponse {
    return httpOk(JsonObject().apply {
        addProperty("name", IntegrationHandler.momentaryVirtualScreen?.type?.routeName)
        addProperty("showingSplash", mc.overlay is SplashOverlay)
    })
}

// POST /api/v1/client/virtualScreen
fun postVirtualScreen(requestObject: RequestObject): FullHttpResponse {
    val body = requestObject.asJson<JsonObject>()
    val name = body["name"]?.asString ?: return httpForbidden("No name")

    val virtualScreen = IntegrationHandler.momentaryVirtualScreen
    if ((virtualScreen?.type?.routeName ?: "none") != name) {
        return httpForbidden("Wrong virtual screen")
    }

    IntegrationHandler.acknowledgement.confirm()
    return httpOk(JsonObject())
}

// GET /api/v1/client/screen
@Suppress("UNUSED_PARAMETER")
fun getScreenInfo(requestObject: RequestObject): FullHttpResponse {
    val mcScreen = mc.currentScreen ?: return httpForbidden("No screen")
    val name = VirtualScreenType.entries.find { it.recognizer(mcScreen) }?.routeName
        ?: mcScreen::class.qualifiedName

    return httpOk(JsonObject().apply {
        addProperty("name", name)
    })
}

// GET /api/v1/client/screen/size
@Suppress("UNUSED_PARAMETER")
fun getScreenSize(requestObject: RequestObject): FullHttpResponse {
    return httpOk(JsonObject().apply {
        addProperty("width", mc.window.scaledWidth)
        addProperty("height", mc.window.scaledHeight)
    })
}

// PUT /api/v1/client/screen
fun putScreen(requestObject: RequestObject): FullHttpResponse {
    val body = requestObject.asJson<JsonObject>()
    val screenName = body["name"]?.asString ?: return httpForbidden("No screen name")

    VirtualScreenType.byName(screenName)?.open()
        ?: return httpForbidden("No screen with name $screenName")
    return httpOk(JsonObject())
}
