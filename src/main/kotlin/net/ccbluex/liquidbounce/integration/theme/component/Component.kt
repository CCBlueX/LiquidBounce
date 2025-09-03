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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.render.Alignment
import java.util.*

/**
 * Represents a HUD component
 */
abstract class Component(
    name: String,
    enabled: Boolean,
    alignment: Alignment = Alignment.center(),
    val tweaks: Array<ComponentTweak> = emptyArray()
) : ToggleableConfigurable(parent = ModuleHud, name = name, enabled = enabled) {

    val id: UUID = UUID.randomUUID()
    val alignment = tree(alignment)

    protected fun registerComponentListen(configurable: Configurable) {
        for (v in configurable.inner) {
            if (v is Configurable) {
                registerComponentListen(v)
            } else {
                v.onChanged {
                    ComponentManager.updateComponents()
                }
            }
        }
    }

}
