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

package net.ccbluex.liquidbounce.web.theme.type.web.components

import net.ccbluex.liquidbounce.web.theme.component.Component
import net.ccbluex.liquidbounce.web.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.web.theme.type.Theme

/**
 * Unlike other components integrated are built into the theme and are being configured
 * by the metadata of the theme
 *
 * TODO: These should be serializable from the Metadata JSON
 */
class IntegratedComponent(
    theme: Theme,
    name: String,
    val tweaks: Array<ComponentTweak> = emptyArray()
) : Component(theme, name, true) {

    init {
        registerComponentListen()
    }

}
