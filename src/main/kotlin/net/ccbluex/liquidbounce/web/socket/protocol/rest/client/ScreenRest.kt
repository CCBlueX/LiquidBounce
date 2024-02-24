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

package net.ccbluex.liquidbounce.web.socket.protocol.rest.client

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.client.gui.screen.SplashOverlay

/**
 * The Screen RestAPI can be used to detect the current screen and virtual screen
 *
 * These are VERY useful for the web client to know what the user is currently doing
 * and which virtual screen is currently active to show the correct page.
 */
fun RestNode.screenRest() {
    get("/virtualScreen") {
        httpOk(JsonObject().apply {
            addProperty("name", IntegrationHandler.momentaryVirtualScreen?.type?.routeName)
            addProperty("showingSplash", mc.overlay is SplashOverlay)
        })
    }

    post("/virtualScreen") {
        val body = decode<JsonObject>(it.content)
        val name = body["name"]?.asString ?: return@post httpForbidden("No name")

        val virtualScreen = IntegrationHandler.momentaryVirtualScreen

        if ((virtualScreen?.type?.routeName ?: "none") != name) {
            return@post httpForbidden("Wrong virtual screen")
        }

        IntegrationHandler.acknowledgement.confirm()
        httpOk(JsonObject())
    }

    get("/screen") {
        val mcScreen = mc.currentScreen ?: return@get httpForbidden("No screen")
        val name = VirtualScreenType.entries.find { it.recognizer(mcScreen) }?.routeName
            ?: mcScreen::class.qualifiedName

        httpOk(JsonObject().apply {
            addProperty("name", name)
        })
    }.apply {
        get("/size") {
            httpOk(JsonObject().apply {
                addProperty("width", mc.window.scaledWidth)
                addProperty("height", mc.window.scaledHeight)
            })
        }
    }

    put("/screen") {
        val body = decode<JsonObject>(it.content)
        val screenName = body["name"]?.asString ?: return@put httpForbidden("No screen name")

        VirtualScreenType.byName(screenName)?.open()
            ?: return@put httpForbidden("No screen with name $screenName")
        httpOk(JsonObject())
    }

}

