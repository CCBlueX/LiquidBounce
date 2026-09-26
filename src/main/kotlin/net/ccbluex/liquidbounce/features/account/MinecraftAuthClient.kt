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

package net.ccbluex.liquidbounce.features.account

import net.ccbluex.liquidbounce.api.core.HttpClient
import net.raphimc.minecraftauth.MinecraftAuth
import net.lenni0451.commons.httpclient.HttpClient as MinecraftAuthHttpClient

/**
 * MinecraftAuth builds its requests on its own HTTP stack rather than OkHttp, so it cannot share
 * [HttpClient.defaultClient]. It does take the user agent, which is passed through here so that
 * account requests are identifiable the same way the rest of the client's traffic is.
 */
internal val minecraftAuthHttpClient: MinecraftAuthHttpClient by lazy {
    MinecraftAuth.createHttpClient(HttpClient.DEFAULT_AGENT)
}
