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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.config.types.NamedChoice

enum class Category(override val choiceName: String) : NamedChoice {

    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc"),
    EXPLOIT("Exploit"),
    FUN("Fun"),

    /**
     * A temporary category for client-related modules, since we don't have a client settings UI yet.
     */
    CLIENT("Client");

    @Deprecated(
        message = "For script compatibility only. Use choiceName instead",
        replaceWith = ReplaceWith("choiceName"),
        level = DeprecationLevel.ERROR
    )
    val readableName: String
        get() = choiceName

    companion object {
        /**
         * Gets an enum by its readable name
         */
        @JvmStatic
        fun fromReadableName(name: String): Category? {
            return entries.find { name.equals(it.name, true) }
        }
    }

}
