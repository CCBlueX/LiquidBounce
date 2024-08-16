/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.network.AbstractClientPlayerEntity

/**
 * Focus module
 *
 * Filters out any other entity to be targeted except the one focus is set to
 */
object ModuleFocus : Module("Focus", Category.MISC) {

    private val usernames by textArray("Usernames", mutableListOf("Notch"))

    /**
     * This option will only focus the enemy on combat modules
     */
    private val combatOnly by boolean("Combat", false)

    val tagEntityEvent = handler<TagEntityEvent> {
        if (it.entity !is AbstractClientPlayerEntity || isInFocus(it.entity)) {
            return@handler
        }

        if (combatOnly) {
            it.dontTarget()
        } else {
           it.ignore()
        }
    }

    /**
     * Check if [entity] is in your focus
     */
    fun isInFocus(entity: AbstractClientPlayerEntity): Boolean {
        if (!enabled) {
            return false
        }

        val name = entity.gameProfile.name

        return usernames.any { it.equals(name, ignoreCase = true) }
    }

}
