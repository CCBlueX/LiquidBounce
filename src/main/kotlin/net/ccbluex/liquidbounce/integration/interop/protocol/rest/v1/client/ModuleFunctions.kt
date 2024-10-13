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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfigurable
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.ccbluex.liquidbounce.integration.interop.protocol.protocolGson
import net.ccbluex.liquidbounce.integration.interop.protocol.genericProtocolGson
import java.io.StringReader

// GET /api/v1/client/modules
@Suppress("UNUSED_PARAMETER")
fun getModules(requestObject: RequestObject): FullHttpResponse {
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
            add("aliases", protocolGson.toJsonTree(module.aliases))
        })
    }
    return httpOk(mods)
}

// PUT /api/v1/client/modules/toggle
// DELETE /api/v1/client/modules/toggle
// POST /api/v1/client/modules/toggle
fun toggleModule(requestObject: RequestObject): FullHttpResponse {
    return requestObject.asJson<ModuleRequest>().acceptToggle(requestObject.method)
}

// GET /api/v1/client/modules/settings
fun getSettings(requestObject: RequestObject): FullHttpResponse {
    return ModuleRequest(requestObject.queryParams["name"] ?: "").acceptGetSettingsRequest()
}

// PUT /api/v1/client/modules/settings
fun putSettings(requestObject: RequestObject): FullHttpResponse {
    return ModuleRequest(requestObject.queryParams["name"] ?: "").acceptPutSettingsRequest(requestObject.body)
}

// POST /api/v1/client/modules/panic
@Suppress("UNUSED_PARAMETER")
fun postPanic(requestObject: RequestObject): FullHttpResponse {
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
    return httpOk(JsonObject())
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
        return httpOk(ConfigSystem.serializeConfigurable(module, gson = genericProtocolGson))
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
