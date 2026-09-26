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

package net.ccbluex.liquidbounce.config.gson.serializer.minecraft

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.api.core.formatAvatarUrl
import net.ccbluex.liquidbounce.features.account.SessionWithService
import net.ccbluex.liquidbounce.features.account.couldBeOnline
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.User
import java.lang.reflect.Type

val User.accountType
    get() = when {
        !this.couldBeOnline()
            || profileId == null
            || mc.isOfflineDeveloperMode -> "legacy" // cracked
        xuid.isEmpty || clientId.isEmpty -> "mojang" // Mojang account
        xuid.isPresent || clientId.isPresent -> "msa" // Microsoft account
        else -> "legacy"
    }

object SessionSerializer : JsonSerializer<User> {
    override fun serialize(src: User?, typeOfSrc: Type, context: JsonSerializationContext) = src?.let {
        JsonObject().apply {
            val service = SessionWithService.getService(src)

            addProperty("username", it.name)
            addProperty("uuid", it.profileId.toString())
            addProperty("service", service.tag)
            addProperty("type", it.accountType)
            addProperty("avatar", formatAvatarUrl(it.profileId, it.name))
            addProperty("online", service.canJoinOnline)
            addProperty("premium", service.canJoinOnline) // todo: deprecated, kept for compatibility
        }
    }
}
