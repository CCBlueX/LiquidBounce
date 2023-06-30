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

package net.ccbluex.liquidbounce.config.adapter

import com.google.gson.*
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.manage.AccountSerializer
import java.lang.reflect.Type

object MinecraftAccountSerializer : JsonSerializer<MinecraftAccount>, JsonDeserializer<MinecraftAccount> {

    override fun serialize(src: MinecraftAccount, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return AccountSerializer.toJson(src)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): MinecraftAccount {
        return AccountSerializer.fromJson(json.asJsonObject)
    }

}
