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
 */

package net.ccbluex.liquidbounce.config.gson.serializer.minecraft

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.api.core.formatAvatarUrl
import net.ccbluex.liquidbounce.utils.client.isPremium
import net.minecraft.client.session.Session
import java.lang.reflect.Type

object SessionSerializer : JsonSerializer<Session> {
    override fun serialize(src: Session?, typeOfSrc: Type, context: JsonSerializationContext) = src?.let {
        JsonObject().apply {
            addProperty("username", it.username)
            addProperty("uuid", it.uuidOrNull.toString())
            addProperty("accountType", it.accountType.getName())
            addProperty("avatar", formatAvatarUrl(it.uuidOrNull, it.username))
            addProperty("premium", it.isPremium())
        }
    }
}
