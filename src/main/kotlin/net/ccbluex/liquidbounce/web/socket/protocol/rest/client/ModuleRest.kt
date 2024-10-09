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
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfigurable
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import net.ccbluex.liquidbounce.web.socket.protocol.strippedProtocolGson
import java.io.StringReader

internal fun RestNode.moduleRest() {
    get("/modules") {
        val mods = JsonArray()
        for (module in ModuleManager) {
            mods.add(JsonObject().apply {
                addProperty("name", module.name)
                addProperty("category", module.category.readableName)
                add("keyBind", protocolGson.toJsonTree(module.bind))
                addProperty("enabled", module.enabled)
                addProperty("description", module.description)
                addProperty("tag", module.tag)
                addProperty("hidden", module.hidden)
                add("aliases", protocolGson.toJsonTree(module.aliases))
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
                AutoConfig.loadingNow = true

                runCatching {
                    for (module in ModuleManager) {
                        if (module.category == Category.RENDER || module.category == Category.CLIENT) {
                            continue
                        }

                        module.enabled = false
                    }

                    ConfigSystem.storeConfigurable(modulesConfigurable)
                }.onFailure {
                    logger.error("Failed to panic disable modules", it)
                }

                AutoConfig.loadingNow = false
            }
            httpOk(JsonObject())
        }

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
            runCatching {
                module.enabled = supposedNew

                ConfigSystem.storeConfigurable(modulesConfigurable)
            }.onFailure {
                logger.error("Failed to toggle module $name", it)
            }
        }
        return httpOk(JsonObject())
    }

    fun acceptGetSettingsRequest(): FullHttpResponse {
        val module = ModuleManager[name] ?: return httpForbidden("$name not found")
        return httpOk(ConfigSystem.serializeConfigurable(module, gson = strippedProtocolGson))
    }

    fun acceptPutSettingsRequest(content: String): FullHttpResponse {
        val module = ModuleManager[name] ?: return httpForbidden("$name not found")

        StringReader(content).use {
            ConfigSystem.deserializeConfigurable(module, it)
        }

        ConfigSystem.storeConfigurable(modulesConfigurable)
        return httpOk(JsonObject())
    }

}
