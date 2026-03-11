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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk

@Suppress("UNUSED_PARAMETER")
fun getSpooferConfig(request: RequestObject): FullHttpResponse {
    // Serialize MultiplayerConfigurable to JSON
    return httpOk(ConfigSystem.serializeValueGroup(SpooferManager, gson = interopGson))
}

fun putSpooferConfig(request: RequestObject): FullHttpResponse {
    ConfigSystem.deserializeValueGroup(SpooferManager, request.body.reader())
    ConfigSystem.store(SpooferManager)
    return httpNoContent()
}
