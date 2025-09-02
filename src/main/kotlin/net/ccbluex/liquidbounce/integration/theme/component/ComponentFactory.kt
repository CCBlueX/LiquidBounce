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

package net.ccbluex.liquidbounce.integration.theme.component

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.theme.component.components.WebComponent
import net.ccbluex.liquidbounce.utils.render.Alignment

abstract class ComponentFactory {

    abstract val name: String
    abstract val enabled: Boolean

    class JsonComponentFactory(
        override val name: String,
        override val enabled: Boolean,
        private val alignment: JsonObject,
        private val tweaks: Array<ComponentTweak>?,
        private val values: Array<JsonObject>?,
    ) : ComponentFactory() {
        override fun createComponent(): WebComponent =
            WebComponent(
                name,
                enabled,
                // todo: replace with deserialization
                Alignment(
                    Alignment.ScreenAxisX.entries.find { it.choiceName == alignment.get("horizontalAlignment").asString }!!,
//                    publicGson.fromJson(alignment.get("horizontalAlignment"), Alignment.ScreenAxisX::class.java),
                    alignment.get("horizontalOffset").asInt,
                    Alignment.ScreenAxisY.entries.find { it.choiceName == alignment.get("verticalAlignment").asString }!!,
//                    publicGson.fromJson(alignment.get("verticalAlignment"), Alignment.ScreenAxisY::class.java),
                    alignment.get("verticalOffset").asInt,
                ),
                tweaks ?: emptyArray(),
                values ?: emptyArray()
            )
    }

    class NativeComponentFactory(
        override val name: String,
        override val enabled: Boolean = false,
        private val function: () -> Component,
    ) : ComponentFactory() {
        override fun createComponent() = function()
    }

    abstract fun createComponent(): Component

}
