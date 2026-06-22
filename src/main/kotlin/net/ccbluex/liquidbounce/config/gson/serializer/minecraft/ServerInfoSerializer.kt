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
import net.minecraft.SharedConstants
import net.minecraft.client.multiplayer.ServerData
import java.lang.reflect.Type
import java.util.Base64

object ServerInfoSerializer : JsonSerializer<ServerData> {
    override fun serialize(src: ServerData, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("name", src.name)
        addProperty("address", src.ip)
        addProperty("status", src.state().name)
        add("playerList", context.serialize(src.playerList))
        add("label", context.serialize(src.motd))
        add("playerCountLabel", context.serialize(src.status))
        add("version", context.serialize(src.version))
        addProperty("protocolVersion", src.protocol)
        addProperty("protocolVersionMatches", src.protocol == SharedConstants.getCurrentVersion().protocolVersion())
        addProperty("ping", src.ping)
        add("players", JsonObject().apply {
            addProperty("max", src.players?.max)
            addProperty("online", src.players?.online)
        })
        addProperty("resourcePackPolicy", ResourcePolicy.fromMinecraftPolicy(src.resourcePackStatus).policyName)

        src.iconBytes?.let {
            addProperty("icon", Base64.getEncoder().encodeToString(it))
        }
    }

}

enum class ResourcePolicy(val policyName: String) {
    PROMPT("Prompt"), ENABLED("Enabled"), DISABLED("Disabled");

    fun toMinecraftPolicy() = when (this) {
        PROMPT -> ServerData.ServerPackStatus.PROMPT
        ENABLED -> ServerData.ServerPackStatus.ENABLED
        DISABLED -> ServerData.ServerPackStatus.DISABLED
    }

    companion object {
        fun fromMinecraftPolicy(policy: ServerData.ServerPackStatus) = when (policy) {
            ServerData.ServerPackStatus.PROMPT -> PROMPT
            ServerData.ServerPackStatus.ENABLED -> ENABLED
            ServerData.ServerPackStatus.DISABLED -> DISABLED
        }

        fun fromString(policy: String) = entries.find { it.policyName == policy }

    }

}
