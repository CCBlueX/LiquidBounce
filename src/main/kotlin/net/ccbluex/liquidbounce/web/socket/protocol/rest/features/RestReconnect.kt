/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.web.socket.protocol.rest.features

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.features.Reconnect
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

fun RestNode.reconnectRest() {
    post("/reconnect") {
        Reconnect.reconnectNow()
        httpOk(JsonObject())
    }

    post("/reconnectWithRandomAccount") {
        Reconnect.reconnectWithRandomPremiumAccount()
        httpOk(JsonObject())
    }

    post("/reconnectWithRandomUsername") {
        Reconnect.reconnectWithRandomCracked()
        httpOk(JsonObject())
    }
}
