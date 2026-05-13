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
import net.ccbluex.netty.http.routing.RoutingContext
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
fun RoutingContext.getModules() {
    val mods = JsonArray(ModuleManager.size)
    for (module in ModuleManager) {
        mods.add(module.toJsonObject())
    }
    respond(mods)
}

// GET /api/v1/client/module/:name
fun RoutingContext.getModule() {
    val name = parameters["name"] ?: forbidden("Module not found")
    val module = ModuleManager[name] ?: forbidden("Module not found")

    respond(module.toJsonObject())
}

// PUT /api/v1/client/modules/toggle
// DELETE /api/v1/client/modules/toggle
// POST /api/v1/client/modules/toggle
suspend fun RoutingContext.toggleModule() {
    with(receive<ModuleRequest>()) {
        acceptToggle(method)
    }
}

// GET /api/v1/client/modules/settings
fun RoutingContext.getSettings() {
    val name = queryParameters["name"] ?: badRequest("Missing parameter 'name'")
    val module = ModuleManager[name] ?: forbidden("Module '$name' not found")
    respond(ConfigSystem.serializeValueGroup(module, gson = interopGson))
}

// PUT /api/v1/client/modules/settings
suspend fun RoutingContext.putSettings() {
    val name = queryParameters["name"] ?: badRequest("Missing parameter 'name'")
    val module = ModuleManager[name] ?: forbidden("Module '$name' not found")
    withContext(Dispatchers.Minecraft) {
        ConfigSystem.deserializeValueGroup(module, CharSequenceReader(body))
        ConfigSystem.store(modulesConfig)

        respondNoContent()
    }
}

// POST /api/v1/client/modules/panic
suspend fun RoutingContext.postPanic() = withContext(Dispatchers.Minecraft) {
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

    respondNoContent()
}

@JvmRecord
private data class ModuleRequest(val name: String) {

    suspend fun RoutingContext.acceptToggle(method: HttpMethod) {
        val module = ModuleManager[name] ?: forbidden("Module '$name' not found")

        val supposedNew = method == HttpMethod.PUT || (method == HttpMethod.POST && !module.enabled)

        if (module.enabled == supposedNew) {
            forbidden("$name already ${if (supposedNew) "enabled" else "disabled"}")
        }

        withContext(Dispatchers.Minecraft) {
            try {
                module.enabled = supposedNew

                ConfigSystem.store(modulesConfig)
            } catch (e: Exception) {
                logger.error("Failed to toggle module $name", e)
            }
        }

        respondNoContent()
    }

}
