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
package net.ccbluex.liquidbounce.web.interop.protocol.rest.v1.client

import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.ClientUpdate
import net.ccbluex.liquidbounce.utils.client.hasProtocolTranslator
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.util.Util
import java.net.URI
import java.text.SimpleDateFormat
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
    addProperty("inGame", inGame)
    addProperty("viaFabricPlus", usesViaFabricPlus)
    addProperty("hasProtocolHack", hasProtocolTranslator)
})

// GET /api/v1/client/update
@Suppress("UNUSED_PARAMETER")
fun getUpdateInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("updateAvailable", LiquidBounce.updateAvailable)
    addProperty("development", LiquidBounce.IN_DEVELOPMENT)
    addProperty("commit", LiquidBounce.clientCommit)

    add("newestVersion", JsonObject().apply {
        val newestVersion = ClientUpdate.newestVersion ?: return@apply

        addProperty("buildId", newestVersion.buildId)
        addProperty("commitId", newestVersion.commitId.substring(0, 7))
        addProperty("branch", newestVersion.branch)
        addProperty("clientVersion", newestVersion.lbVersion)
        addProperty("minecraftVersion", newestVersion.mcVersion)
        addProperty("release", newestVersion.release)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestVersion.date)
        addProperty("date", SimpleDateFormat().format(dateFormat))
        addProperty("message", newestVersion.message)

        addProperty("url", newestVersion.url)
    })
})

// POST /api/v1/client/exit
@Suppress("UNUSED_PARAMETER")
fun postExit(requestObject: RequestObject): FullHttpResponse {
    mc.scheduleStop()
    return httpOk(JsonObject())
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
    return httpOk(JsonObject())
}

private val POSSIBLE_URL_TARGETS: Map<String, URI> = run {
    val properties = Properties()

    properties.load(LiquidBounce::class.java.getResourceAsStream("/assets/liquidbounce/client_urls.properties"))

    properties.stringPropertyNames().associateWith { URI(properties.getProperty(it)) }
}
