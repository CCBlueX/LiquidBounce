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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffectInstance
import java.lang.reflect.Type

object StatusEffectInstanceSerializer : JsonSerializer<MobEffectInstance> {
    override fun serialize(
        src: MobEffectInstance?, typeOfSrc: Type, context: JsonSerializationContext
    ) = src?.let {
        JsonObject().apply {
            addProperty("effect", BuiltInRegistries.MOB_EFFECT.getKey(it.effect.value()).toString())
            addProperty("localizedName", it.effect.value().displayName.string)
            addProperty("duration", it.duration)
            addProperty("amplifier", it.amplifier)
            addProperty("ambient", it.isAmbient)
            addProperty("infinite", it.isInfiniteDuration)
            addProperty("visible", it.isVisible)
            addProperty("showIcon", it.showIcon())
            addProperty("color", it.effect.value().color)
        }
    }

}
