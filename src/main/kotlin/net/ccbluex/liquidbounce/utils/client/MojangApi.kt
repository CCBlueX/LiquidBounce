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
 */
package net.ccbluex.liquidbounce.utils.client

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.utils.io.HttpClient

object MojangApi {

    /**
     * Get UUID of username
     */
    fun getUUID(username: String): String {
        // TODO: Use GameProfileSerializer from authlib

        // Read response content and get id from json
        try {
            // Make an http connection to Mojang API and ask for UUID of username
            val text = HttpClient.get("https://api.mojang.com/users/profiles/minecraft/$username")

            val jsonElement = JsonParser().parse(text)

            if (jsonElement.isJsonObject) {
                return jsonElement.asJsonObject.get("id").asString
            }
        } catch (e: Exception) {
            return ""
        }
        return ""
    }

}
