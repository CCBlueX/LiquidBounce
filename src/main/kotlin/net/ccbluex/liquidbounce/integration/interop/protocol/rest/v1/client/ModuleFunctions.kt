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
import net.ccbluex.netty.http.application.ApplicationCall
import net.ccbluex.netty.http.routing.Routing
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
private fun Routing.getModules() = get {
    val mods = JsonArray(ModuleManager.size)
    for (module in ModuleManager) {
        mods.add(module.toJsonObject())
    }
    call.respond(mods)
}

// GET /api/v1/client/module/:name
private fun Routing.getModule() = get("/module/:name") {
    val name = call.parameters["name"] ?: call.forbidden("Module not found")
    val module = ModuleManager[name] ?: call.forbidden("Module not found")

    call.respond(module.toJsonObject())
}

// POST /api/v1/client/modules/toggle
private fun Routing.toggleModulePost() = post {
    call.receive<ModuleRequest>().handle(call)
}

// PUT /api/v1/client/modules/toggle
private fun Routing.toggleModulePut() = put {
    call.receive<ModuleRequest>().handle(call)
}

// DELETE /api/v1/client/modules/toggle
private fun Routing.toggleModuleDelete() = delete {
    call.receive<ModuleRequest>().handle(call)
}

// GET /api/v1/client/modules/settings
private fun Routing.getSettings() = get {
    val name = call.queryParameters["name"] ?: call.badRequest("Missing parameter 'name'")
    val module = ModuleManager[name] ?: call.forbidden("Module '$name' not found")
    call.respond(ConfigSystem.serializeValueGroup(module, gson = interopGson))
}

// PUT /api/v1/client/modules/settings
private fun Routing.putSettings() = put {
    val name = call.queryParameters["name"] ?: call.badRequest("Missing parameter 'name'")
    val module = ModuleManager[name] ?: call.forbidden("Module '$name' not found")
    withContext(Dispatchers.Minecraft) {
        ConfigSystem.deserializeValueGroup(module, CharSequenceReader(call.body))
        ConfigSystem.store(modulesConfig)

        call.respondNoContent()
    }
}

// POST /api/v1/client/modules/panic
private fun Routing.postPanic() = post("/panic") { withContext(Dispatchers.Minecraft) {
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

    call.respondNoContent()
} }

@JvmRecord
private data class ModuleRequest(val name: String) {
    suspend fun handle(call: ApplicationCall) {
        val module = ModuleManager[this.name] ?: call.forbidden("Module '${this.name}' not found")
        val supposedNew = call.method == HttpMethod.PUT || (call.method == HttpMethod.POST && !module.enabled)
        if (module.enabled == supposedNew) {
            call.forbidden("${this.name} already ${if (supposedNew) "enabled" else "disabled"}")
        }
        withContext(Dispatchers.Minecraft) {
            try {
                module.enabled = supposedNew
                ConfigSystem.store(modulesConfig)
            } catch (e: Exception) {
                logger.error("Failed to toggle module ${this@ModuleRequest.name}", e)
            }
        }
        call.respondNoContent()
    }
}

internal fun Routing.moduleRoutes() {
    route("/modules") {
        getModules()
        route("/toggle") {
            toggleModulePut()
            toggleModuleDelete()
            toggleModulePost()
        }
        route("/settings") {
            getSettings()
            putSettings()
        }
        postPanic()
    }
    getModule()
}
