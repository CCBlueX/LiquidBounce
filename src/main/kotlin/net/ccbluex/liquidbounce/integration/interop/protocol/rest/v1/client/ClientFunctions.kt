/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.FileDialogMode
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.*
import net.minecraft.util.Util
import java.io.File
import java.net.URI
import java.util.*

// GET /api/v1/client/info
@Suppress("UNUSED_PARAMETER")
fun getClientInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("gameVersion", mc.gameVersion)
    addProperty("clientVersion", LiquidBounce.clientVersion)
    addProperty("clientName", LiquidBounce.CLIENT_NAME)
    addProperty("development", LiquidBounce.IN_DEVELOPMENT)
    addProperty("fps", mc.currentFps)
    addProperty("gameDir", mc.runDirectory.path)
    addProperty("clientDir", ConfigSystem.rootFolder.path)
    addProperty("inGame", inGame)
    addProperty("viaFabricPlus", usesViaFabricPlus)
    addProperty("hasProtocolHack", usesViaFabricPlus)
})

// GET /api/v1/client/update
@Suppress("UNUSED_PARAMETER")
fun getUpdateInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("development", LiquidBounce.IN_DEVELOPMENT)
    addProperty("commit", LiquidBounce.clientCommit)

    val updateInfo = update ?: return@apply
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

// POST /api/v1/client/exit
@Suppress("UNUSED_PARAMETER")
fun postExit(requestObject: RequestObject): FullHttpResponse {
    mc.scheduleStop()
    return httpNoContent()
}

// GET /api/v1/client/window
@Suppress("UNUSED_PARAMETER")
fun getWindowInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("width", mc.window.width)
    addProperty("height", mc.window.height)
    addProperty("scaledWidth", mc.window.scaledWidth)
    addProperty("scaledHeight", mc.window.scaledHeight)
    addProperty("scaleFactor", mc.window.scaleFactor)
    addProperty("guiScale", mc.options.guiScale.value)
})

// POST /api/v1/client/browse
fun postBrowse(requestObject: RequestObject): FullHttpResponse {
    val jsonObj = requestObject.asJson<JsonObject>()
    val target = jsonObj["target"]?.asString ?: return httpForbidden("No target specified")

    val url = POSSIBLE_URL_TARGETS[target] ?: return httpForbidden("Unknown target")

    Util.getOperatingSystem().open(url)
    return httpNoContent()
}

// POST /api/v1/client/browsePath
@Suppress("ReturnCount")
fun postBrowsePath(requestObject: RequestObject): FullHttpResponse {
    val jsonObj = requestObject.asJson<JsonObject>()
    val path = jsonObj["path"]?.asString ?: return httpBadRequest("No file specified")

    val file = File(path).let { file ->
        if (file.isAbsolute) file else ConfigSystem.rootFolder.resolve(file)
    }

    if (!file.exists()) {
        return httpNotFound(path, "File not exists")
    }

    // Ensures we open a directory, not a file
    val directoryToOpen = when {
        file.isDirectory -> file
        file.isFile -> file.parentFile ?: return httpForbidden("Cannot access root directory")
        else -> return httpForbidden("Invalid file type")
    }

    Util.getOperatingSystem().open(directoryToOpen)
    return httpNoContent()
}

// POST /api/v1/client/fileDialog
fun postFileDialog(requestObject: RequestObject): FullHttpResponse {
    data class RequestBody(val mode: FileDialogMode, val supportedExtensions: List<String>? = null)
    val (mode, supportedExtensions) = runCatching {
        requestObject.asJson<RequestBody>()
    }.getOrNull() ?: return httpBadRequest("No dialog mode provided")

    val files = mode.selectFiles(supportedExtensions)

    return httpOk(JsonObject().apply {
        files.firstOrNull()?.let { addProperty("file", it) }
    })
}

private val POSSIBLE_URL_TARGETS: Map<String, URI> = run {
    val properties = Properties()

    properties.load(LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/client_urls.properties"))

    properties.stringPropertyNames().associateWith { URI(properties.getProperty(it)) }
}
