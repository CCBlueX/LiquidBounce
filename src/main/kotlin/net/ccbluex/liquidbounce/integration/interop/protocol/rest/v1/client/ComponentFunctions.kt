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

import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.types.FileValue
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfig
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.notFound
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.respondJsonWriter
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.render.Alignment
import org.apache.commons.io.input.CharSequenceReader
import java.util.Locale

// GET /api/v1/client/components/native
private fun Route.getNativeComponents() = get("/native") {
    call.respond(accessibleInteropGson.toJsonTree(
        HudComponentManager.nativeComponents
    ))
}

// GET /api/v1/client/components/{id}
private fun Route.getComponents() = get("/{id}") {
    call.respond(accessibleInteropGson.toJsonTree(
        HudComponentManager.getComponents(call.parameters["id"]))
    )
}

// GET /api/v1/client/components/{id}/catalog
private fun Route.getComponentCatalog() = get("/{id}/catalog") {
    call.respond(accessibleInteropGson.toJsonTree(
        HudComponentManager.getComponentCatalog(call.parameters["id"].orEmpty())
    ))
}

// POST /api/v1/client/components/{id}
private fun Route.postComponent() = post("/{id}") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")

    withContext(Dispatchers.Minecraft) {
        HudComponentManager.addComponent(id)
            ?: call.badRequest("HUD component cannot be added again")
        ConfigSystem.store(modulesConfig)
    }
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/components/{id}/z-index
private fun Route.postComponentZIndex() = post("/{id}/z-index") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    val component = HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")

    val zIndex = withContext(Dispatchers.Minecraft) {
        HudComponentManager.bringComponentToFront(component).also {
            ConfigSystem.store(modulesConfig)
        }
    }

    call.respondJsonWriter {
        beginObject()
        name("zIndex").value(zIndex)
        endObject()
    }
}

// POST /api/v1/client/components/{id}/alignment
private fun Route.postComponentAlignment() = post("/{id}/alignment") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    val component = HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")
    val alignment = runCatching {
        requireNotNull(accessibleInteropGson.fromJson(call.receiveText(), Alignment::class.java))
    }.getOrElse {
        call.badRequest("Invalid alignment")
    }

    withContext(Dispatchers.Minecraft) {
        component.alignment.setFrom(alignment)
        ConfigSystem.store(modulesConfig)
    }
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// GET /api/v1/client/components/{id}/settings
private fun Route.getComponentSettings() = get("/{id}/settings") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    val component = HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")

    call.respond(ConfigSystem.serializeValueGroup(component, gson = interopGson))
}

// GET /api/v1/client/components/{id}/file
private fun Route.getComponentFile() = get("/{id}/file") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    // Strict check, only default theme component 'Image' uses this feature currently
    val component = HudComponentManager.getComponent(id)
        ?.takeIf { it.name == "Image" }
        ?: call.notFound(id, "Image HUD component not found")
    val fileValue = component.collectValuesRecursively()
        .filterIsInstance<FileValue>()
        .find { it.name == "File" }
        ?: call.notFound(id, "Image file setting not found")
    val file = fileValue.absoluteFile

    if (!file.isFile) {
        call.notFound(file.path, "File not found")
    }

    val supportedExtensions = fileValue.supportedExtensions
    if (supportedExtensions != null && supportedExtensions.none { extension ->
            extension.equals(file.extension, ignoreCase = true)
        }) {
        call.badRequest("Unsupported file extension: ${file.extension.lowercase(Locale.ROOT)}")
    }

    call.respondFile(file)
}

// PUT /api/v1/client/components/{id}/settings
private fun Route.putComponentSettings() = put("/{id}/settings") {
    val id = call.parameters["id"] ?: call.badRequest("Missing component id")
    val component = HudComponentManager.getComponent(id)
        ?: call.notFound(id, "HUD component not found")

    withContext(Dispatchers.Minecraft) {
        val wasEnabled = component.enabled
        ConfigSystem.deserializeValueGroup(component, CharSequenceReader(call.receiveText()))
        if (wasEnabled && !component.enabled) {
            component.resetAlignment()
        }
        ConfigSystem.store(modulesConfig)
    }
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

internal fun Route.componentRoutes() = route("/components") {
    getNativeComponents()
    getComponentCatalog()
    getComponents()
    postComponent()
    postComponentZIndex()
    postComponentAlignment()
    getComponentSettings()
    getComponentFile()
    putComponentSettings()
}
