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
 *
 */

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.render.Alignment

/**
 * Represents a HUD component
 */
abstract class Component(
    name: String,
    enabled: Boolean,
    alignment: Alignment = Alignment.center()
) : ToggleableConfigurable(parent = ComponentOverlay, name = name, enabled = enabled) {

    val alignment = tree(alignment)

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
