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

package net.ccbluex.liquidbounce.web.socket.protocol.rest.module

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

internal fun RestNode.setupModuleRestApi() {
    get("/modules") {
        val mods = JsonArray()
        for (module in ModuleManager) {
            mods.add(JsonObject().apply {
                addProperty("name", module.name)
                addProperty("category", module.category.name)
                addProperty("keyBind", module.bind)
                addProperty("enabled", module.enabled)
                addProperty("description", module.description)
                addProperty("tag", module.tag)
                addProperty("hidden", module.hidden)
            })
        }
        httpOk(mods)
    }
}
