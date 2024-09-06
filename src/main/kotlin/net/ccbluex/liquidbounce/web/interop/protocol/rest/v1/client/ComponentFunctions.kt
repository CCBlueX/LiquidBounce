/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

import net.ccbluex.liquidbounce.web.interop.protocol.protocolGson
import net.ccbluex.liquidbounce.web.theme.component.components
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk

// GET /api/v1/client/components
@Suppress("UNUSED_PARAMETER")
fun getComponents(requestObject: RequestObject) =
    httpOk(protocolGson.toJsonTree(components).asJsonArray)
