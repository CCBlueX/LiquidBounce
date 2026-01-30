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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfig
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
import org.apache.commons.io.input.CharSequenceReader

private fun ClientModule.toJsonObject() = JsonObject().apply {
    addProperty("name", name)
    addProperty("category", category.tag)
    add("keyBind", interopGson.toJsonTree(bind))
    addProperty("enabled", enabled)
    addProperty("description", description.get())
    addProperty("tag", this@toJsonObject.tag)
    addProperty("hidden", hidden)
    add("aliases", interopGson.toJsonTree(aliases))
}

// GET /api/v1/client/modules
@Suppress("UNUSED_PARAMETER")
fun getModules(requestObject: RequestObject): FullHttpResponse {
    val mods = JsonArray(ModuleManager.size)
    for (module in ModuleManager) {
        mods.add(module.toJsonObject())
    }
    return httpOk(mods)
}

// GET /api/v1/client/module/:name
fun getModule(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"] ?: return httpForbidden("Module not found")
    val module = ModuleManager[name] ?: return httpForbidden("Module not found")

    return httpOk(module.toJsonObject())
}

// PUT /api/v1/client/modules/toggle
// DELETE /api/v1/client/modules/toggle
// POST /api/v1/client/modules/toggle
suspend fun toggleModule(requestObject: RequestObject): FullHttpResponse {
    return requestObject.asJson<ModuleRequest>().acceptToggle(requestObject.method)
}

// GET /api/v1/client/modules/settings
fun getSettings(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.queryParams["name"] ?: return httpBadRequest("Missing parameter 'name'")
    val module = ModuleManager[name] ?: return httpForbidden("Module '$name' not found")
    return httpOk(ConfigSystem.serializeValueGroup(module, gson = interopGson))
}

// PUT /api/v1/client/modules/settings
suspend fun putSettings(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.queryParams["name"] ?: return httpBadRequest("Missing parameter 'name'")
    val module = ModuleManager[name] ?: return httpForbidden("Module '$name' not found")
    return withContext(Dispatchers.Minecraft) {
        ConfigSystem.deserializeValueGroup(module, CharSequenceReader(requestObject.body))
        ConfigSystem.store(modulesConfig)

        httpNoContent()
    }
}

// POST /api/v1/client/modules/panic
@Suppress("UNUSED_PARAMETER")
suspend fun postPanic(requestObject: RequestObject): FullHttpResponse = withContext(Dispatchers.Minecraft) {
    AutoConfig.withLoading {
        runCatching {
            for (module in ModuleManager) {
                if (module.category == ModuleCategories.RENDER) {
                    continue
                }

                module.enabled = false
            }

            ConfigSystem.store(modulesConfig)
        }.onFailure {
            logger.error("Failed to panic disable modules", it)
        }
    }

    httpNoContent()
}

@JvmRecord
private data class ModuleRequest(val name: String) {

    suspend fun acceptToggle(method: HttpMethod): FullHttpResponse {
        val module = ModuleManager[name] ?: return httpForbidden("Module '$name' not found")

        val supposedNew = method == HttpMethod.PUT || (method == HttpMethod.POST && !module.enabled)

        if (module.enabled == supposedNew) {
            return httpForbidden("$name already ${if (supposedNew) "enabled" else "disabled"}")
        }

        withContext(Dispatchers.Minecraft) {
            try {
                module.enabled = supposedNew

                ConfigSystem.store(modulesConfig)
            } catch (e: Exception) {
                logger.error("Failed to toggle module $name", e)
            }
        }

        return httpNoContent()
    }

}
