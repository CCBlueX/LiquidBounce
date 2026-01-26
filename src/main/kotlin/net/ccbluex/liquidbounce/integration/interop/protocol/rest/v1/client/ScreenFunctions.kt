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
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.client.gui.screens.LoadingOverlay
import net.minecraft.client.gui.screens.TitleScreen

// GET /api/v1/client/virtualScreen
@Suppress("UNUSED_PARAMETER")
fun getVirtualScreenInfo(requestObject: RequestObject): FullHttpResponse {
    return httpOk(JsonObject().apply {
        addProperty("name", ScreenManager.screen?.type?.routeName)
        addProperty("showingSplash", mc.overlay is LoadingOverlay)
    })
}

// POST /api/v1/client/virtualScreen
fun postVirtualScreen(requestObject: RequestObject): FullHttpResponse {
    val body = requestObject.asJson<JsonObject>()
    val name = body["name"]?.asString ?: return httpForbidden("No name")

    val virtualScreen = ScreenManager.screen
    if ((virtualScreen?.type?.routeName ?: "none") != name) {
        return httpForbidden("Wrong virtual screen")
    }

    ScreenManager.screenAcknowledgement.confirm()
    return httpNoContent()
}

// GET /api/v1/client/screen
@Suppress("UNUSED_PARAMETER")
fun getScreenInfo(requestObject: RequestObject): FullHttpResponse {
    val mcScreen = mc.screen ?: return httpForbidden("No screen")
    val name = CustomScreenType.recognize(mcScreen)?.routeName ?: mcScreen::class.qualifiedName

    return httpOk(JsonObject().apply {
        addProperty("name", name)
    })
}

// GET /api/v1/client/screen/size
@Suppress("UNUSED_PARAMETER")
fun getScreenSize(requestObject: RequestObject): FullHttpResponse {
    return httpOk(JsonObject().apply {
        addProperty("width", mc.window.guiScaledWidth)
        addProperty("height", mc.window.guiScaledHeight)
    })
}

// PUT /api/v1/client/screen
fun putScreen(requestObject: RequestObject): FullHttpResponse {
    val body = requestObject.asJson<JsonObject>()
    val screenName = body["name"]?.asString ?: return httpForbidden("No screen name")

    CustomScreenType.byName(screenName)?.open()
        ?: return httpForbidden("No screen with name $screenName")
    return httpNoContent()
}

// DELETE /api/v1/client/screen
@Suppress("UNUSED_PARAMETER")
fun deleteScreen(requestObject: RequestObject): FullHttpResponse {
    val screen = mc.screen ?: return httpForbidden("No screen")

    if (screen is CustomSharedMinecraftScreen && screen.parentScreen != null) {
        mc.execute {
            mc.setScreen(screen.parentScreen)
        }
        return httpNoContent()
    }

    mc.execute {
        mc.setScreen(
            if (inGame) {
                null
            } else {
                TitleScreen()
            }
        )
    }
    return httpNoContent()
}
