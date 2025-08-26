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
 *
 */

package net.ccbluex.liquidbounce.config.gson.serializer.minecraft

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.minecraft.SharedConstants
import net.minecraft.client.network.ServerInfo
import java.lang.reflect.Type
import java.util.*

object ServerInfoSerializer : JsonSerializer<ServerInfo> {
    override fun serialize(src: ServerInfo, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("name", src.name)
        addProperty("address", src.address)
        addProperty("status", src.status.name)
        add("playerList", context.serialize(src.playerListSummary))
        add("label", context.serialize(src.label))
        add("playerCountLabel", context.serialize(src.playerCountLabel))
        add("version", context.serialize(src.version))
        addProperty("protocolVersion", src.protocolVersion)
        addProperty("protocolVersionMatches", src.protocolVersion == SharedConstants.getGameVersion().protocolVersion)
        addProperty("ping", src.ping)
        add("players", JsonObject().apply {
            addProperty("max", src.players?.max)
            addProperty("online", src.players?.online)
        })
        addProperty("resourcePackPolicy", ResourcePolicy.fromMinecraftPolicy(src.resourcePackPolicy).policyName)

        src.favicon?.let {
            addProperty("icon", Base64.getEncoder().encodeToString(it))
        }
    }

}

enum class ResourcePolicy(val policyName: String) {
    PROMPT("Prompt"), ENABLED("Enabled"), DISABLED("Disabled");

    fun toMinecraftPolicy() = when (this) {
        PROMPT -> ServerInfo.ResourcePackPolicy.PROMPT
        ENABLED -> ServerInfo.ResourcePackPolicy.ENABLED
        DISABLED -> ServerInfo.ResourcePackPolicy.DISABLED
    }

    companion object {
        fun fromMinecraftPolicy(policy: ServerInfo.ResourcePackPolicy) = when (policy) {
            ServerInfo.ResourcePackPolicy.PROMPT -> PROMPT
            ServerInfo.ResourcePackPolicy.ENABLED -> ENABLED
            ServerInfo.ResourcePackPolicy.DISABLED -> DISABLED
        }

        fun fromString(policy: String) = entries.find { it.policyName == policy }

    }

}
