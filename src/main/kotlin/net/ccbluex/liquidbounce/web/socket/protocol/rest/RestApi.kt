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
package net.ccbluex.liquidbounce.web.socket.protocol.rest

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RouteController
import net.ccbluex.liquidbounce.web.socket.protocol.rest.client.*
import net.ccbluex.liquidbounce.web.socket.protocol.rest.features.containerRest
import net.ccbluex.liquidbounce.web.socket.protocol.rest.features.protocolRest
import net.ccbluex.liquidbounce.web.socket.protocol.rest.features.reconnectRest
import net.ccbluex.liquidbounce.web.socket.protocol.rest.game.*
import net.ccbluex.liquidbounce.web.socket.protocol.rest.game.ServerListRest.serverListRest

class RestApi {

    fun setupRoutes() {
        RouteController
            .new("/api/v1/client").apply {
                // Client RestAPI
                clientRest()
                themeRest()
                localStorageRest()
                moduleRest()
                proxyRest()
                configRest()
                screenRest()
                sessionRest()
                accountsRest()
                componentRest()

                // Feature RestAPI
                containerRest()
                protocolRest()
                reconnectRest()

                // Game RestAPI
                playerRest()
                registriesRest()
                serverListRest()
                worldListRest()
                resourceRest()
                inputRest()
            }

        RouteController.get("/") {
            httpOk(JsonObject().apply {
                addProperty("name", LiquidBounce.CLIENT_NAME)
                addProperty("version", LiquidBounce.clientVersion)
                addProperty("author", LiquidBounce.CLIENT_AUTHOR)
            })
        }

        RouteController.file("/", ConfigSystem.rootFolder.resolve("themes"))
    }

}
