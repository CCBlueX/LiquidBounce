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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfig
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.netty.http.routing.Routing
import org.apache.commons.io.input.CharSequenceReader

// GET /api/v1/client/components/native
private fun Routing.getNativeComponents() = get("/native") {
    call.respond(
        HudComponentManager.nativeComponents,
        accessibleInteropGson,
    )
}

// GET /api/v1/client/components/:id
private fun Routing.getComponents() = get("/:id") {
    call.respond(
        HudComponentManager.getComponents(call.parameters["id"]),
        accessibleInteropGson,
    )
}

// GET /api/v1/client/components/:id/catalog
private fun Routing.getComponentCatalog() = get("/:id/catalog") {
    call.respond(
        HudComponentManager.getComponentCatalog(call.parameters["id"].orEmpty()),
        accessibleInteropGson,
    )
}

// POST /api/v1/client/components/:id
private fun Routing.postComponent() = post("/:id") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")

    withContext(Dispatchers.Minecraft) {
        HudComponentManager.addComponent(id)
            ?: call.badRequest("HUD component cannot be added again")
        ConfigSystem.store(modulesConfig)
    }
    call.respondNoContent()
}

// POST /api/v1/client/components/:id/alignment
private fun Routing.postComponentAlignment() = post("/:id/alignment") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    val component = HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")
    val alignment = runCatching {
        requireNotNull(accessibleInteropGson.fromJson(call.body, Alignment::class.java))
    }.getOrElse {
        call.badRequest("Invalid alignment")
    }

    withContext(Dispatchers.Minecraft) {
        component.alignment.setFrom(alignment)
        ConfigSystem.store(modulesConfig)
    }
    call.respondNoContent()
}

// GET /api/v1/client/components/:id/settings
private fun Routing.getComponentSettings() = get("/:id/settings") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    val component = HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")

    call.respond(ConfigSystem.serializeValueGroup(component, gson = interopGson))
}

// PUT /api/v1/client/components/:id/settings
private fun Routing.putComponentSettings() = put("/:id/settings") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    val component = HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")

    withContext(Dispatchers.Minecraft) {
        val wasEnabled = component.enabled
        ConfigSystem.deserializeValueGroup(component, CharSequenceReader(call.body))
        if (wasEnabled && !component.enabled) {
            component.resetAlignment()
        }
        ConfigSystem.store(modulesConfig)
    }
    call.respondNoContent()
}

internal fun Routing.componentRoutes() = route("/components") {
    getNativeComponents()
    getComponentCatalog()
    getComponents()
    postComponent()
    postComponentAlignment()
    getComponentSettings()
    putComponentSettings()
}
