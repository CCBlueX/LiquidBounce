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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.accountRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.clientRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.componentRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.globalRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.localStorageRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.marketplaceRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.moduleRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.proxyRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.screenRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.sessionRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.spooferRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.themeRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.userRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.browserRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.protocolRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features.reconnectRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.inputRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.playerRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.registryRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.serverListRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.textureRoutes
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.worldListRoutes

internal fun Route.registerInteropFunctions() = route("/api/v1/client") {
    clientRoutes()
    userRoutes()
    localStorageRoutes()
    themeRoutes()
    screenRoutes()
    moduleRoutes()
    componentRoutes()
    sessionRoutes()
    accountRoutes()
    proxyRoutes()
    browserRoutes()
    protocolRoutes()
    reconnectRoutes()
    spooferRoutes()
    globalRoutes()
    inputRoutes()
    playerRoutes()
    registryRoutes()
    serverListRoutes()
    textureRoutes()
    worldListRoutes()
    marketplaceRoutes()
}
