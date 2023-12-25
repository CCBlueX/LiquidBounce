/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.web.socket.protocol.rest.client.setupClientRestApi
import net.ccbluex.liquidbounce.web.socket.protocol.rest.game.setupPlayerRestApi
import net.ccbluex.liquidbounce.web.socket.protocol.rest.game.setupServerApi
import net.ccbluex.liquidbounce.web.socket.protocol.rest.game.setupWorldApi
import net.ccbluex.liquidbounce.web.socket.protocol.rest.client.module.setupModuleRestApi
import net.ccbluex.liquidbounce.web.socket.protocol.rest.client.session.setupSessionRestApi

class RestApi {

    fun setupRoutes() {
        RouteController
            .new("/api/v1/client").apply {
                setupClientRestApi()
                setupSessionRestApi()
                setupModuleRestApi()
                setupWorldApi()
                setupServerApi()
                setupPlayerRestApi()
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
