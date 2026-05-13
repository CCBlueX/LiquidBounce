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
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.routing.RoutingContext
import net.minecraft.client.gui.screens.LoadingOverlay
import net.minecraft.client.gui.screens.TitleScreen

// GET /api/v1/client/virtualScreen
fun RoutingContext.getVirtualScreenInfo() {
    respond(JsonObject().apply {
        addProperty("name", ScreenManager.screen?.type?.routeName)
        addProperty("showingSplash", mc.overlay is LoadingOverlay)
    })
}

// POST /api/v1/client/virtualScreen
fun RoutingContext.postVirtualScreen() {
    val payload = receive<JsonObject>()
    val name = payload["name"]?.asString ?: forbidden("No name")

    val virtualScreen = ScreenManager.screen
    if ((virtualScreen?.type?.routeName ?: "none") != name) {
        forbidden("Wrong virtual screen")
    }

    ScreenManager.screenAcknowledgement.confirm()
    respondNoContent()
}

// GET /api/v1/client/screen
fun RoutingContext.getScreenInfo() {
    val mcScreen = mc.screen ?: forbidden("No screen")
    val name = CustomScreenType.recognize(mcScreen)?.routeName ?: mcScreen::class.qualifiedName

    respond(JsonObject().apply {
        addProperty("name", name)
    })
}

// GET /api/v1/client/screen/size
fun RoutingContext.getScreenSize() {
    respond(JsonObject().apply {
        addProperty("width", mc.window.guiScaledWidth)
        addProperty("height", mc.window.guiScaledHeight)
    })
}

// PUT /api/v1/client/screen
fun RoutingContext.putScreen() {
    val payload = receive<JsonObject>()
    val screenName = payload["name"]?.asString ?: forbidden("No screen name")

    CustomScreenType.byName(screenName)?.open()
        ?: forbidden("No screen with name $screenName")
    respondNoContent()
}

// DELETE /api/v1/client/screen
fun RoutingContext.deleteScreen() {
    val screen = mc.screen ?: forbidden("No screen")

    if (screen is CustomSharedMinecraftScreen && screen.parentScreen != null) {
        mc.execute {
            mc.setScreen(screen.parentScreen)
        }
        respondNoContent()
        return
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
    respondNoContent()
}
