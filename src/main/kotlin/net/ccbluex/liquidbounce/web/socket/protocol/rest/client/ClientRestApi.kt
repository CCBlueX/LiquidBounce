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

package net.ccbluex.liquidbounce.web.socket.protocol.rest.client

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

internal fun RestNode.setupClientRestApi() {
    get("/info") {
        httpOk(JsonObject().apply {
            addProperty("gameVersion", mc.gameVersion)
            addProperty("clientVersion", LiquidBounce.clientVersion)
            addProperty("clientName", LiquidBounce.CLIENT_NAME)
            addProperty("fps", mc.currentFps)
            addProperty("gameDir", mc.runDirectory.path)
        })
    }

    get("/exit") {
        mc.scheduleStop()
        httpOk(JsonObject())
    }

    get("/virtualScreen") {
        httpOk(JsonObject().apply {
            addProperty("name", IntegrationHandler.momentaryVirtualScreen?.name)
        })
    }
}
