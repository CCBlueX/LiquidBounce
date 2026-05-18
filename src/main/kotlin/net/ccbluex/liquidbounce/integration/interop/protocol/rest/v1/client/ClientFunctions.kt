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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.FileDialogMode
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.netty.http.routing.Routing
import net.minecraft.util.Util
import java.io.File
import java.net.URI
import java.util.Properties

// GET /api/v1/client/info
private fun Routing.getClientInfo() = get("/info") {
    call.respond(JsonObject().apply {
        addProperty("os", Util.getPlatform().telemetryName())
        addProperty("gameVersion", mc.launchedVersion)
        addProperty("clientVersion", LiquidBounce.clientVersion)
        addProperty("clientName", LiquidBounce.CLIENT_NAME)
        addProperty("development", LiquidBounce.IN_DEVELOPMENT)
        addProperty("fps", mc.fps)
        addProperty("gameDir", mc.gameDirectory.path)
        addProperty("clientDir", ConfigSystem.rootFolder.path)
        addProperty("inGame", inGame)
        addProperty("viaFabricPlus", usesViaFabricPlus)
        addProperty("hasProtocolHack", usesViaFabricPlus)
    })
}

// GET /api/v1/client/update
@Suppress("ReturnCount")
private fun Routing.getUpdateInfo() = get("/update") {
    call.respond(JsonObject().apply {
        addProperty("development", LiquidBounce.IN_DEVELOPMENT)
        addProperty("commit", LiquidBounce.clientCommit)

        val updateInfo = update.await() ?: return@apply
        add("update", JsonObject().apply {
            addProperty("buildId", updateInfo.buildId)
            addProperty("commitId", updateInfo.commitId.substring(0, 7))
            addProperty("branch", updateInfo.branch)
            addProperty("clientVersion", updateInfo.lbVersion)
            addProperty("minecraftVersion", updateInfo.mcVersion)
            addProperty("release", updateInfo.release)

            addProperty("date", updateInfo.date.toString())
            addProperty("message", updateInfo.message)

            addProperty("url", updateInfo.url)
        })
    })
}

// POST /api/v1/client/exit
private fun Routing.postExit() = post("/exit") {
    mc.stop()
    call.respondNoContent()
}

// GET /api/v1/client/window
private fun Routing.getWindowInfo() = get("/window") {
    call.respond(JsonObject().apply {
        addProperty("width", mc.window.screenWidth)
        addProperty("height", mc.window.screenHeight)
        addProperty("scaledWidth", mc.window.guiScaledWidth)
        addProperty("scaledHeight", mc.window.guiScaledHeight)
        addProperty("scaleFactor", mc.window.guiScale)
        addProperty("guiScale", mc.options.guiScale().get())
    })
}

// POST /api/v1/client/browse
private fun Routing.postBrowse() = post("/browse") {
    val jsonObj = call.receive<JsonObject>()
    val target = jsonObj["target"]?.asString ?: call.forbidden("No target specified")

    val url = POSSIBLE_URL_TARGETS[target] ?: call.forbidden("Unknown target")

    Util.getPlatform().openUri(url)
    call.respondNoContent()
}

// POST /api/v1/client/browsePath
@Suppress("ReturnCount")
private fun Routing.postBrowsePath() = post("/browsePath") {
    val jsonObj = call.receive<JsonObject>()
    val path = jsonObj["path"]?.asString ?: call.badRequest("No file specified")

    val file = File(path).let { file ->
        if (file.isAbsolute) file else ConfigSystem.rootFolder.resolve(file)
    }

    if (!file.exists()) {
        call.notFound(path, "File not exists")
    }

    // Ensures we open a directory, not a file
    val directoryToOpen = when {
        file.isDirectory -> file
        file.isFile -> file.parentFile ?: call.forbidden("Cannot access root directory")
        else -> call.forbidden("Invalid file type")
    }

    Util.getPlatform().openFile(directoryToOpen)
    call.respondNoContent()
}

// POST /api/v1/client/fileDialog
private fun Routing.postFileDialog() = post("/fileDialog") {
    data class RequestBody(val mode: FileDialogMode, val supportedExtensions: List<String>? = null)
    val (mode, supportedExtensions) = runCatching {
        call.receive<RequestBody>()
    }.getOrNull() ?: call.badRequest("No dialog mode provided")

    val files = mode.selectFiles(supportedExtensions)

    call.respond(JsonObject().apply {
        files.firstOrNull()?.let { addProperty("file", it) }
    })
}

private val POSSIBLE_URL_TARGETS: Map<String, URI> = buildMap {
    val properties = Properties()

    properties.load(LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/client_urls.properties"))

    properties.forEach { (k, v) ->
        this[k as String] = URI(v as String)
    }
}

internal fun Routing.clientRoutes() {
    getClientInfo()
    getUpdateInfo()
    postExit()
    getWindowInfo()
    postBrowse()
    postBrowsePath()
    postFileDialog()
}
