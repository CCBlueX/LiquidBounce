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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ccbluex.liquidbounce.integration.interop.internalServerError
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.readAsBase64
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.gui.screens.NoticeWithLinkScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.world.level.validation.ContentValidationException
import java.io.IOException

private val mutex = Mutex()

// GET /api/v1/client/worlds
private fun Route.getWorlds() = get {
    val worlds = JsonArray()

    try {
        val levelList = mc.levelSource.findLevelCandidates()
        if (levelList.isEmpty) {
            call.respond(worlds)
            return@get
        }

        // Refreshes the list of summaries
        val summaries = mutex.withLock {
            mc.levelSource.loadLevelSummaries(levelList).await()
        }

        for ((index, summary) in summaries.withIndex()) {
            worlds.add(JsonObject().apply {
                addProperty("id", index)
                addProperty("name", summary.levelId)
                addProperty("displayName", summary.levelName)
                addProperty("lastPlayed", summary.lastPlayed)
                addProperty("gameMode", summary.settings.gameType().serializedName)
                addProperty("difficulty", summary.settings.difficultySettings.difficulty.serializedName)
                addProperty("icon", runCatching { summary.icon.readAsBase64() }.onFailure {
                    //logger.error("Failed to read icon for world ${summary.name}", it)
                }.getOrNull())
                addProperty("version", summary.levelVersion().minecraftVersionName())
                addProperty("hardcore", summary.settings.difficultySettings.hardcore())
                addProperty("commandsAllowed", summary.settings.allowCommands())
                addProperty("locked", summary.isLocked)
                addProperty("requiresConversion", summary.requiresManualConversion())
                addProperty("isVersionAvailable", summary.isCompatible)
                addProperty("shouldPromptBackup", summary.shouldBackup())
                addProperty("wouldBeDowngraded", summary.isDowngrade)
            })
        }
        call.respond(worlds)
    } catch (e: Exception) {
        call.internalServerError("Failed to get worlds due to ${e.message}")
    }
}

// POST /api/v1/client/worlds/join
private fun Route.postJoinWorld() = post("/join") {
    val request = call.receive<LevelRequest>()

    mc.execute {
        runCatching {
            mc.createWorldOpenFlows().openWorld(request.name) {
                mc.gui.setScreen(SelectWorldScreen(TitleScreen()))
            }
        }.onFailure {
            logger.error("Failed to join world ${request.name}", it)
        }
    }

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/worlds/edit
private fun Route.postEditWorld() = post("/edit") {
    val request = call.receive<LevelRequest>()

    mc.execute {
        val session = runCatching {
            mc.levelSource.validateAndCreateAccess(request.name)
        }.onFailure { exception ->
            when (exception) {
                is IOException -> {
                    SystemToast.onWorldAccessFailure(mc, request.name)
                    logger.error("Failed to access level ${request.name}", exception)
                }

                is ContentValidationException -> {
                    logger.warn(exception.message)
                    mc.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen { mc.gui.setScreen(
                        SelectWorldScreen(
                            TitleScreen())) })
                }

                else -> {
                    logger.error("Failed to access level ${request.name}", exception)
                }
            }
        }.getOrNull() ?: return@execute

        runCatching {
            EditWorldScreen.create(mc, session) { _ ->
                session.safeClose()
                mc.gui.setScreen(SelectWorldScreen(TitleScreen()))
            }
        }.onFailure { exception ->
            session.safeClose()
            SystemToast.onWorldAccessFailure(mc, request.name)
            logger.error("Failed to load world data ${request.name}", exception)
        }.onSuccess { screen ->
            mc.gui.setScreen(screen)
        }
    }

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/worlds/delete
private fun Route.postDeleteWorld() = post("/delete") {
    val request = call.receive<LevelRequest>()

    runCatching {
        mc.levelSource.createAccess(request.name).use { session ->
            session.deleteLevel()
        }
    }.onFailure {
        logger.error("Failed to delete world ${request.name}", it)
    }

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

private data class LevelRequest(val name: String)

internal fun Route.worldListRoutes() = route("/worlds") {
    getWorlds()
    postJoinWorld()
    postEditWorld()
    postDeleteWorld()
}
