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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import java.io.StringReader

internal fun RestNode.setupModuleRestApi() {
    get("/modules") {
        val mods = JsonArray()
        for (module in ModuleManager) {
            mods.add(JsonObject().apply {
                addProperty("name", module.name)
                addProperty("category", module.category.readableName)
                addProperty("keyBind", module.bind)
                addProperty("enabled", module.enabled)
                addProperty("description", module.description)
                addProperty("tag", module.tag)
                addProperty("hidden", module.hidden)
            })
        }
        httpOk(mods)
    }.apply {
        put("/toggle") {
            decode<ModuleRequest>(it.content)
                .acceptToggle(it.context.httpMethod)
        }

        delete("/toggle") {
            decode<ModuleRequest>(it.content)
                .acceptToggle(it.context.httpMethod)
        }

        post("/toggle") {
            decode<ModuleRequest>(it.content)
                .acceptToggle(it.context.httpMethod)
        }

        get("/settings") {
            ModuleRequest(it.params["name"] ?: "")
                .acceptGetSettingsRequest()
        }

        put("/settings") {
            ModuleRequest(it.params["name"] ?: "")
                .acceptPutSettingsRequest(it.content)
        }

        post("/panic") {
            RenderSystem.recordRenderCall {
                for (module in ModuleManager) {
                    if (module.category == Category.RENDER) {
                        continue
                    }

                    module.enabled = false
                }
            }
            httpOk(JsonObject())
        }

    }
}

internal fun RestNode.setupOptions() {
    // clickgui settings
    get("/options/clickgui") {
        val clickGui = ModuleClickGui

        httpOk(clickGui.settingsAsJson())
    }
}

data class ModuleRequest(val name: String) {

    fun acceptToggle(method: HttpMethod): FullHttpResponse {
        val module = ModuleManager[name] ?: return httpForbidden("$name not found")

        val supposedNew = method == HttpMethod.PUT || (method == HttpMethod.POST && !module.enabled)

        if (module.enabled == supposedNew) {
            return httpForbidden("$name already ${if (supposedNew) "enabled" else "disabled"}")
        }

        RenderSystem.recordRenderCall {
            module.enabled = supposedNew
        }
        return httpOk(JsonObject())
    }

    fun acceptGetSettingsRequest(): FullHttpResponse {
        val module = ModuleManager[name] ?: return httpForbidden("$name not found")
        return httpOk(ConfigSystem.serializeConfigurable(module, gson = protocolGson))
    }

    fun acceptPutSettingsRequest(content: String): FullHttpResponse {
        val module = ModuleManager[name] ?: return httpForbidden("$name not found")

        StringReader(content).use {
            ConfigSystem.deserializeConfigurable(module, it)
        }

        return httpOk(JsonObject())
    }

}
