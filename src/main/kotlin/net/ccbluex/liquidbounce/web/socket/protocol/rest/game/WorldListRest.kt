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
package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpInternalServerError
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.readImageAsBase64
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.world.EditWorldScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.screen.world.SymlinkWarningScreen
import net.minecraft.client.toast.SystemToast
import net.minecraft.util.path.SymlinkValidationException
import net.minecraft.world.level.storage.LevelSummary
import java.io.IOException

var summaries = emptyList<LevelSummary>()

internal fun RestNode.worldListRest() {
    get("/worlds") {
        val worlds = JsonArray()

        runCatching {
            val levelList = mc.levelStorage.levelList
            if (levelList.isEmpty) {
                return@get httpOk(worlds)
            }

            // Refreshes the list of summaries
            summaries = mc.levelStorage.loadSummaries(levelList).get()

            for ((index, summary) in summaries.withIndex()) {
                worlds.add(JsonObject().apply {
                    addProperty("id", index)
                    addProperty("name", summary.name)
                    addProperty("displayName", summary.displayName)
                    addProperty("lastPlayed", summary.lastPlayed)
                    addProperty("gameMode", summary.levelInfo.gameMode.getName())
                    addProperty("difficulty", summary.levelInfo.difficulty.getName())
                    addProperty("icon", runCatching { readImageAsBase64(summary.iconPath) }.onFailure {
                        //logger.error("Failed to read icon for world ${summary.name}", it)
                    }.getOrNull())
                    addProperty("version", summary.versionInfo.versionName)
                    addProperty("hardcore", summary.levelInfo.isHardcore)
                    addProperty("commandsAllowed", summary.levelInfo.areCommandsAllowed())
                    addProperty("locked", summary.isLocked)
                    addProperty("requiresConversion", summary.requiresConversion())
                    addProperty("isVersionAvailable", summary.isVersionAvailable)
                    addProperty("shouldPromptBackup", summary.shouldPromptBackup())
                    addProperty("wouldBeDowngraded", summary.wouldBeDowngraded())
                })
            }
            httpOk(worlds)
        }.getOrElse { httpInternalServerError("Failed to get worlds due to ${it.message}") }
    }.apply {
        data class LevelRequest(val name: String)

        post("/join") {
            val request = decode<LevelRequest>(it.content)

            RenderSystem.recordRenderCall {
                runCatching {
                    mc.createIntegratedServerLoader().start(request.name) {
                        mc.setScreen(SelectWorldScreen(TitleScreen()))
                    }
                }.onFailure {
                    logger.error("Failed to join world ${request.name}", it)
                }
            }

            httpOk(JsonObject())
        }

        post("/edit") {
            val request = decode<LevelRequest>(it.content)

            RenderSystem.recordRenderCall {
                val session = runCatching {
                    mc.levelStorage.createSession(request.name)
                }.onFailure { exception ->
                    when (exception) {
                        is IOException -> {
                            SystemToast.addWorldAccessFailureToast(mc, request.name)
                            logger.error("Failed to access level ${request.name}", exception)
                        }
                        is SymlinkValidationException -> {
                            logger.warn(exception.message)
                            mc.setScreen(SymlinkWarningScreen.world { mc.setScreen(SelectWorldScreen(TitleScreen())) })
                        }
                        else -> {
                            logger.error("Failed to access level ${request.name}", exception)
                        }
                    }
                }.getOrNull() ?: return@recordRenderCall

                runCatching {
                    EditWorldScreen.create(mc, session) { _ ->
                        session.tryClose()
                        mc.setScreen(SelectWorldScreen(TitleScreen()))
                    }
                }.onFailure { exception ->
                    session.tryClose()
                    SystemToast.addWorldAccessFailureToast(mc, request.name)
                    logger.error("Failed to load world data ${request.name}", exception)
                }.onSuccess { screen ->
                    mc.setScreen(screen)
                }
            }

            httpOk(JsonObject())
        }

        post("/delete") {
            val request = decode<LevelRequest>(it.content)

            runCatching {
                mc.levelStorage.createSessionWithoutSymlinkCheck(request.name).use { session ->
                    session.deleteSessionLock()
                }
            }.onFailure {
                logger.error("Failed to delete world ${request.name}", it)
            }

            httpOk(JsonObject())
        }
    }
}


