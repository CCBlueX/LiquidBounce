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
package net.ccbluex.liquidbounce.features.module

object ModuleCategories {

    private val registry = mutableListOf<ModuleCategory>()

    val COMBAT = register(ModuleCategory("Combat"))
    val PLAYER = register(ModuleCategory("Player"))
    val MOVEMENT = register(ModuleCategory("Movement"))
    val RENDER = register(ModuleCategory("Render"))
    val WORLD = register(ModuleCategory("World"))
    val MISC = register(ModuleCategory("Misc"))
    val EXPLOIT = register(ModuleCategory("Exploit"))
    val FUN = register(ModuleCategory("Fun"))

    /**
     * A temporary category for client-related modules, since we don't have a client settings UI yet.
     */
    val CLIENT = register(ModuleCategory("Client"))

    val entries: List<ModuleCategory> = registry

    @JvmStatic
    private fun register(category: ModuleCategory): ModuleCategory {
        registry.add(category)
        return category
    }

    @JvmStatic
    fun byName(name: String): ModuleCategory? {
        return registry.firstOrNull { it.choiceName.equals(name, ignoreCase = true) }
    }

}
