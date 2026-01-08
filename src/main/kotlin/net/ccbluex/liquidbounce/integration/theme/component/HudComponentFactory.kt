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

package net.ccbluex.liquidbounce.integration.theme.component

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.integration.theme.component.components.WebHudComponent
import net.ccbluex.liquidbounce.utils.render.Alignment

abstract class HudComponentFactory {

    abstract val name: String
    abstract val enabled: Boolean
    abstract val singleton: Boolean

    /**
     * Factory for creating components from JSON deserialization.
     *
     * @param name Component name
     * @param enabled Whether the component is enabled
     * @param alignment JSON alignment data
     * @param tweaks Optional tweaks array
     * @param values Optional values array
     */
    class JsonHudComponentFactory(
        override val name: String,
        override val enabled: Boolean,
        override val singleton: Boolean,
        private val alignment: JsonObject,
        private val tweaks: Array<HudComponentTweak>?,
        private val values: Array<JsonObject>?
    ) : HudComponentFactory() {

        override fun createComponent(): WebHudComponent = WebHudComponent(
            name,
            enabled,
            accessibleInteropGson.fromJson(alignment, Alignment::class.java),
            tweaks ?: emptyArray(),
            values ?: emptyArray()
        )

    }

    /**
     * Factory for creating native components from a function.
     *
     * @param name Component name
     * @param enabled Whether the component is enabled
     * @param function Function producing the component
     */
    class NativeHudComponentFactory(
        override val name: String,
        override val enabled: Boolean = false,
        override val singleton: Boolean = false,
        private val function: () -> HudComponent
    ) : HudComponentFactory() {
        override fun createComponent() = function()
    }

    /**
     * Creates the component instance.
     *
     * @return Component instance
     */
    abstract fun createComponent(): HudComponent
}
