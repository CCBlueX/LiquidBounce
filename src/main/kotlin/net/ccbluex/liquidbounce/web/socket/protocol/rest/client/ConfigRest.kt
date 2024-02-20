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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson

internal fun RestNode.configRest() {
    get("/configs") {
        val jsonArray = JsonArray()

        for (config in AutoConfig.configs) {
            jsonArray.add(protocolGson.toJsonTree(config))
        }

        httpOk(jsonArray)
    }

    put("/config") { request ->
        data class ConfigRequest(val settingId: String)
        val cfgRequest = decode<ConfigRequest>(request.content)

        val config = AutoConfig.configs.find { it.settingId == cfgRequest.settingId }
            ?: return@put httpForbidden("Config not found")

        AutoConfig.loadAutoConfig(config)
        httpOk(JsonObject())
    }

}
