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
package net.ccbluex.liquidbounce.api.services.servermedia

import net.ccbluex.liquidbounce.api.core.BaseApi

/**
 * CCBlueX's mirror of LabyMod's server-media, which collects icons of Minecraft servers.
 */
object ServerMediaApi : BaseApi(BASE_URL) {

    /**
     * @param domains the folder of the server each domain belongs to
     */
    class Index(val domains: Map<String, String>)

    suspend fun getIndex() = get<Index>("/index.json")

    fun iconUrl(folder: String) = "$BASE_URL/minecraft_servers/$folder/icon.png"

}

private const val BASE_URL = "https://server-media.liquidbounce.net"
