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

package net.ccbluex.liquidbounce.config.types.nesting

interface Toggleable {

    var enabled: Boolean

    /**
     * The listener that will be called when the state is toggled. By default it will
     * simply call [onEnabled] or [onDisabled] depending on the state.
     */
    fun onToggled(state: Boolean): Boolean {
        if (state) {
            onEnabled()
        } else {
            onDisabled()
        }

        return state
    }

    /**
     * Will be called when the state is toggled on.
     */
    fun onEnabled() { }

    /**
     * Will be called when the state is toggled off.
     */
    fun onDisabled() { }

}
