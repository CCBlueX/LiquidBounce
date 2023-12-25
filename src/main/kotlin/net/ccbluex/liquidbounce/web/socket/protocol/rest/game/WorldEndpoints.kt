/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpInternalServerError
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import kotlin.io.path.absolutePathString

internal fun RestNode.setupWorldApi() {
    get("/worlds") {
        val worlds = JsonArray()

        runCatching {
            val levelList = mc.levelStorage.levelList
            if (levelList.isEmpty) {
                return@get httpOk(worlds)
            }

            val summaries = mc.levelStorage.loadSummaries(levelList).get()

            for (summary in summaries) {
                worlds.add(JsonObject().apply {
                    addProperty("name", summary.name)
                    addProperty("displayName", summary.displayName)
                    addProperty("lastPlayed", summary.lastPlayed)
                    addProperty("gameMode", summary.levelInfo.gameMode.getName())
                    addProperty("difficulty", summary.levelInfo.difficulty.getName())
                    addProperty("icon", summary.iconPath.absolutePathString())
                    addProperty("hardcore", summary.levelInfo.isHardcore)
                    addProperty("commandsAllowed", summary.levelInfo.areCommandsAllowed())
                })
            }
            httpOk(worlds)
        }.getOrElse { httpInternalServerError("Failed to get worlds due to ${it.message}") }
    }
}
