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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonObject
import com.mojang.blaze3d.Blaze3D
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.FileDialogMode
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ScoreboardData
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.notFound
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.liquidbounce.utils.network.NetworkTelemetry
import net.minecraft.ChatFormatting
import net.minecraft.util.Util
import com.google.gson.JsonArray
import java.util.ArrayDeque
import java.io.File
import java.net.URI
import java.util.Properties

// GET /api/v1/client/info
private fun Route.getClientInfo() = get("/info") {
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
private fun Route.getUpdateInfo() = get("/update") {
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
private fun Route.postExit() = post("/exit") {
    mc.stop()
    call.respond(HttpStatusCode.NoContent)
}

// PUT /api/v1/client/hud-editor
private fun Route.putHudEditorState() = put("/hud-editor") {
    data class Request(val selected: Boolean)
    val selected = call.receive<Request>().selected

    withContext(Dispatchers.Main) {
        ModuleHud.hudEditorSelected = selected
    }
    call.respond(HttpStatusCode.NoContent)
}

// GET /api/v1/client/window
private fun Route.getWindowInfo() = get("/window") {
    call.respond(JsonObject().apply {
        addProperty("width", mc.window.screenWidth)
        addProperty("height", mc.window.screenHeight)
        addProperty("scaledWidth", mc.window.guiScaledWidth)
        addProperty("scaledHeight", mc.window.guiScaledHeight)
        addProperty("scaleFactor", mc.window.guiScale)
        addProperty("guiScale", mc.options.guiScale().get())
    })
}

// POST /api/v1/client/openLink
private fun Route.postOpenLink() = post("/openLink") {
    val jsonObj = call.receive<JsonObject>()
    val target = jsonObj["target"]?.asString ?: call.forbidden("No target specified")

    val url = POSSIBLE_URL_TARGETS[target] ?: call.forbidden("Unknown target")

    Blaze3D.openUri(url)
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/browsePath
@Suppress("ReturnCount")
private fun Route.postBrowsePath() = post("/browsePath") {
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

    Blaze3D.openPath(directoryToOpen.toPath())
    call.respond(HttpStatusCode.NoContent)
}

// POST /api/v1/client/fileDialog
private fun Route.postFileDialog() = post("/fileDialog") {
    data class RequestBody(val mode: FileDialogMode, val supportedExtensions: List<String>? = null)
    val (mode, supportedExtensions) = runCatching {
        call.receive<RequestBody>()
    }.getOrNull() ?: call.badRequest("No dialog mode provided")

    val files = mode.selectFiles(supportedExtensions).await()

    call.respond(JsonObject().apply {
        files.firstOrNull()?.let { addProperty("file", it) }
    })
}

// POST /api/v1/client/command
private fun Route.postCommand() = post("/command") {
    data class Request(val command: String)
    val command = runCatching { call.receive<Request>().command }.getOrNull()
        ?: call.badRequest("No command provided")

    val sent = withContext(Dispatchers.Main) {
        val prefix = CommandManager.GlobalSettings.prefix
        if (command.startsWith(prefix)) {
            CommandManager.execute(command.substring(prefix.length))
            return@withContext true
        }

        val connection = mc.player?.connection ?: return@withContext false
        if (command.startsWith('/')) {
            connection.sendCommand(command.substring(1))
        } else {
            connection.sendChat(command)
        }
        true
    }

    if (!sent) {
        call.badRequest("Not in game")
    }
    call.respond(HttpStatusCode.NoContent)
}

// GET /api/v1/client/player
private fun Route.getPlayerState() = get("/player") {
    val state = withContext(Dispatchers.Main) {
        JsonObject().apply {
            val player = mc.player
            addProperty("inGame", inGame)
            addProperty("screen", mc.gui.screen()?.javaClass?.simpleName)
            add("chat", ChatHistory.toJson())

            val telemetry = NetworkTelemetry.snapshot
            addProperty("setbacks", telemetry.setbacks)
            addProperty("packetsOut", telemetry.packetsOut)
            addProperty("movePacketsPerSecond", telemetry.movePacketsPerSecond)
            addProperty("speedBps", telemetry.speedBps)

            player ?: return@apply
            addProperty("name", player.scoreboardName)
            addProperty("x", player.x)
            addProperty("y", player.y)
            addProperty("z", player.z)
            addProperty("health", player.health)
            addProperty("food", player.foodData.foodLevel)
            addProperty("onGround", player.onGround())
            addProperty("dimension", player.level().dimension().identifier().toString())
            addProperty("gameMode", mc.gameMode?.playerMode?.getName())

            ScoreboardData.fromScoreboard(player.level().scoreboard)?.let { scoreboard ->
                add("scoreboard", JsonObject().apply {
                    addProperty("title", scoreboard.header.string.stripFormatting())
                    add("lines", JsonArray().apply {
                        scoreboard.entries.filterNotNull().forEach { entry ->
                            add(entry.name.string.stripFormatting())
                        }
                    })
                })
            }
        }
    }
    call.respond(state)
}

private fun String.stripFormatting() = ChatFormatting.stripFormatting(this) ?: this

private object ChatHistory : EventListener {

    private const val CAPACITY = 50
    private val lines = ArrayDeque<Pair<Long, String>>(CAPACITY)

    @Suppress("unused")
    private val chatHandler = handler<ChatReceiveEvent> { event ->
        synchronized(lines) {
            if (lines.size == CAPACITY) {
                lines.removeFirst()
            }
            lines.addLast(System.currentTimeMillis() to event.textData.string.stripFormatting())
        }
    }

    fun toJson() = JsonArray().apply {
        synchronized(lines) {
            lines.forEach { (time, text) ->
                add(JsonObject().apply {
                    addProperty("timestamp", time)
                    addProperty("message", text)
                })
            }
        }
    }

}

private val POSSIBLE_URL_TARGETS: Map<String, URI> = buildMap {
    val properties = Properties()

    properties.load(LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/client_urls.properties"))

    properties.forEach { (k, v) ->
        this[k as String] = URI(v as String)
    }
}

internal fun Route.clientRoutes() {
    getClientInfo()
    getUpdateInfo()
    postExit()
    putHudEditorState()
    getWindowInfo()
    postOpenLink()
    postBrowsePath()
    postFileDialog()
    postCommand()
    getPlayerState()
    ChatHistory
}
