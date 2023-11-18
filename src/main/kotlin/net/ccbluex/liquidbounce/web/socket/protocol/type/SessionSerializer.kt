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
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.type

import com.google.gson.*
import net.minecraft.client.util.Session
import java.lang.reflect.Type

class SessionSerializer : JsonSerializer<Session> {

    override fun serialize(session: Session?, type: Type?, ctx: JsonSerializationContext?): JsonElement
        = JsonObject().apply {
            if (session == null) {
                return JsonNull.INSTANCE
            }

            addProperty("username", session.username)
            addProperty("uuid", session.uuid)
            addProperty("accountType", session.accountType.getName())
            // DO NOT ADD TOKEN - WE DO NOT WANT TO EXPOSE IT
        }

}
