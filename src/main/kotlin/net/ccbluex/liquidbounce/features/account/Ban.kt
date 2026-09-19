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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.gson.util.long
import net.ccbluex.liquidbounce.config.gson.util.string

/**
 * @param bannedUntil epoch milliseconds the ban expires at, or `-1` if it never does.
 */
data class Ban(val serverName: String, val reason: String, val bannedUntil: Long = -1L) {

    val isPermanent: Boolean
        get() = bannedUntil == -1L

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("serverName", serverName)
        addProperty("reason", reason)
        addProperty("bannedUntil", bannedUntil)
    }

    companion object {
        fun fromJson(json: JsonObject) = Ban(
            serverName = json.string("serverName")
                ?: throw IllegalArgumentException("'$json' is not a valid Ban"),
            reason = json.string("reason")
                ?: throw IllegalArgumentException("'$json' is not a valid Ban"),
            bannedUntil = json.long("bannedUntil") ?: -1L,
        )
    }

}
