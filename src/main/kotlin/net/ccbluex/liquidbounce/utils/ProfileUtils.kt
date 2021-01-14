/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.utils

import com.google.gson.JsonParser

object ProfileUtils {

    /**
     * Get UUID of username
     */
    fun getUUID(username: String): String {
        // TODO: Use GameProfileSerializer from authlib

        // Make a http connection to Mojang API and ask for UUID of username
        val text = HttpUtils.get("https://api.mojang.com/users/profiles/minecraft/$username")

        // Read response content and get id from json
        val jsonElement = JsonParser().parse(text)

        if(jsonElement.isJsonObject) {
            return jsonElement.asJsonObject.get("id").asString
        }
        return ""
    }

}
