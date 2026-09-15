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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager

/**
 * Every registered module, the client's own and those of add-ons. Register yours through
 * [net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon.registerModules].
 */
object Modules {

    @JvmStatic
    val all: Collection<ClientModule>
        get() = ModuleManager

    /**
     * By name or alias, ignoring case.
     */
    @JvmStatic
    fun get(name: String): ClientModule? = ModuleManager[name]

    @JvmStatic
    fun isEnabled(name: String): Boolean = get(name)?.enabled == true

    /**
     * @return false if no module is called [name]
     */
    @JvmStatic
    fun setEnabled(name: String, enabled: Boolean): Boolean {
        val module = get(name) ?: return false
        module.enabled = enabled
        return true
    }

}
