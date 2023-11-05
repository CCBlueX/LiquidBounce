/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.network.AbstractClientPlayerEntity

/**
 * Focus module
 *
 * Filters out any other entity to be targeted except the one focus is set to
 */
object ModuleFocus : Module("Focus", Category.MISC) {

    val usernameFocus by text("Username", "Notch")

    /**
     * This option will only focus the enemy on combat modules
     */
    val combatFocus by boolean("Combat", false)

    /**
     * Check if [entity] is in your focus
     */
    fun isInFocus(entity: AbstractClientPlayerEntity): Boolean {
        if (!enabled) {
            return false
        }

        val name = entity.gameProfile.name
        return name == usernameFocus
    }

}
