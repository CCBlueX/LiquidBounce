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
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpInternalServerError
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.readImageAsBase64
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.world.level.storage.LevelSummary

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
                        logger.error("Failed to read icon for world ${summary.name}", it)
                    }.getOrNull())
                    addProperty("version", summary.versionInfo.versionName)
                    addProperty("hardcore", summary.levelInfo.isHardcore)
                    addProperty("commandsAllowed", summary.levelInfo.areCommandsAllowed())
                })
            }
            httpOk(worlds)
        }.getOrElse { httpInternalServerError("Failed to get worlds due to ${it.message}") }
    }
}


