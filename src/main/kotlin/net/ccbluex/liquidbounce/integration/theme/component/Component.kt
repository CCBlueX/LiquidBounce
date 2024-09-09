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

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.integration.theme.type.Theme

/**
 * Represents a HUD component
 */
open class Component(
    val theme: Theme,
    name: String,
    enabled: Boolean,
    val alignment: Alignment,
    val tweaks: Array<ComponentTweak> = emptyArray()
) : ToggleableConfigurable(parent = ComponentOverlay, name = name, enabled = enabled) {

    init {
        tree(alignment)
    }

    protected fun registerComponentListen(cfg: Configurable = this) {
        for (v in cfg.inner) {
            if (v is Configurable) {
                registerComponentListen(v)
            } else {
                v.onChanged {
                    ComponentOverlay.fireComponentsUpdate()
                }
            }
        }
    }

    override fun parent() = ComponentOverlay

}
