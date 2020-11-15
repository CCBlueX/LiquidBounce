/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2020 CCBlueX
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

package net.ccbluex.liquidbounce.module

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.Listenable
import net.minecraft.client.MinecraftClient

open class Module(val name: String, val category: Category, val bind: String = "", defaultState: Boolean = false) : Listenable {

    var state: Boolean = defaultState
        set(value) {
            field = value
            toggled()
        }

    companion object {
        @JvmStatic
        val mc = MinecraftClient.getInstance()
    }

    // TODO: make event instead of function
    open fun toggled() { }

    /**
     * Registers an event hook for events of type [T]
     */
    inline fun <reified T: Event> handler(ignoreCondition: Boolean = false, noinline eventHandler: (T) -> Unit) {
        LiquidBounce.eventManager.registerEventHook(T::class.java, EventHook(this, eventHandler, ignoreCondition))
    }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state

}