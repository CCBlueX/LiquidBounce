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

package net.ccbluex.liquidbounce.web.socket.protocol

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.config.ConfigSystem.registerCommonTypeAdapters
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.adapter.ConfigurableSerializer
import net.ccbluex.liquidbounce.config.adapter.ProtocolConfigurableSerializer

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ProtocolExclude

class ProtocolExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>?) = false
    override fun shouldSkipField(field: FieldAttributes) = field.getAnnotation(ProtocolExclude::class.java) != null
}

internal val protocolGson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExclusionStrategy())
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ProtocolConfigurableSerializer)
    .create()

