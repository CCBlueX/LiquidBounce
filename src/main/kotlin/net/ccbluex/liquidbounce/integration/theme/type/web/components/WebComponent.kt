/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.integration.theme.type.web.components

import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.web.ComponentMetadata

/**
 * Unlike other components, web components are built into the theme and are being configured
 * by the metadata of the theme
 */
class WebComponent(
    theme: Theme,
    name: String,
    metadata: ComponentMetadata
) : Component(
    theme,
    name,
    metadata.enabled,
    metadata.alignment.toAlignment(),
    tweaks = metadata.tweaks?.mapNotNull { tweak -> ComponentTweak.entries.find { it.name == tweak } }?.toTypedArray() ?: emptyArray()
) {

    init {
        registerComponentListen()
    }

}
