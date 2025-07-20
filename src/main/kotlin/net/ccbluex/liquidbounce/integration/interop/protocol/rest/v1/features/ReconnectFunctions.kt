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
 *
 */
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features

import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk

// POST /api/v1/client/reconnect
@Suppress("UNUSED_PARAMETER")
fun postReconnect(requestObject: RequestObject): FullHttpResponse {
    ServerObserver.reconnect()
    return httpOk(emptyJsonObject())
}
