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
 */

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.integration.interop.protocol.protocolGson
import net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import java.io.StringReader

// GET /api/v1/client/components/:name
@Suppress("UNUSED_PARAMETER")
fun getComponents(requestObject: RequestObject) = httpOk(JsonArray().apply {
    for ((index, component) in components.filter { it.theme.name == requestObject.params["name"] }.withIndex()) {
        add(JsonObject().apply {
            addProperty("id", index)
            addProperty("name", component.name)
            add("alignment", protocolGson.toJsonTree(component.alignment))
        })
    }
})

// GET /api/v1/client/components/:name/:index
@Suppress("UNUSED_PARAMETER")
fun getComponentSettings(requestObject: RequestObject) = httpOk(ConfigSystem.serializeConfigurable(
    components.filter { it.theme.name == requestObject.params["name"] }[requestObject.params["index"]?.toInt() ?: error("No index provided")]
))

// POST /api/v1/client/components/:name/:index
fun updateComponentSettings(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"]
    val index = requestObject.params["index"]?.toInt() ?: error("No index provided")

    val component = components.filter { it.theme.name == name }[index]
    ConfigSystem.deserializeConfigurable(component, StringReader(requestObject.body), gson = protocolGson)
    return httpOk(JsonObject())
}

// PUT /api/v1/client/components/:name/:index
fun moveComponent(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"]
    val index = requestObject.params["index"]?.toInt() ?: error("No index provided")

    val component = components.filter { it.theme.name == name }[index]

    // We copy the alignment to the existing because we do not want to replace the instance
    val newAlignment = protocolGson.fromJson(requestObject.body, Alignment::class.java)
    component.alignment.apply {
        horizontalAlignment = newAlignment.horizontalAlignment
        horizontalOffset = newAlignment.horizontalOffset
        verticalAlignment = newAlignment.verticalAlignment
        verticalOffset = newAlignment.verticalOffset
    }

    return httpOk(JsonObject())
}
