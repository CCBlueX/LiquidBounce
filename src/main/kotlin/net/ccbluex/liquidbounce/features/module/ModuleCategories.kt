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

import java.util.TreeMap

object ModuleCategories {

    private val registry = TreeMap<String, ModuleCategory>(String.CASE_INSENSITIVE_ORDER)

    @JvmField
    val COMBAT = register(ModuleCategory("Combat"))

    @JvmField
    val PLAYER = register(ModuleCategory("Player"))

    @JvmField
    val MOVEMENT = register(ModuleCategory("Movement"))

    @JvmField
    val RENDER = register(ModuleCategory("Render"))

    @JvmField
    val WORLD = register(ModuleCategory("World"))

    @JvmField
    val MISC = register(ModuleCategory("Misc"))

    @JvmField
    val EXPLOIT = register(ModuleCategory("Exploit"))

    @JvmField
    val FUN = register(ModuleCategory("Fun"))

    @JvmStatic
    val entries: Collection<ModuleCategory> get() = registry.sequencedValues()

    @JvmStatic
    private fun register(category: ModuleCategory): ModuleCategory {
        if (registry.put(category.tag, category) != null) {
            error("A module category with the name '${category.tag}' is already registered!")
        }

        return category
    }

    @JvmStatic
    fun byName(name: String): ModuleCategory? {
        return registry[name]
    }

}
