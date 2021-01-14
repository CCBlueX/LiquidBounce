/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW

open class Module(val name: String, val category: Category, val bind: Int = GLFW.GLFW_KEY_UNKNOWN,
                  defaultState: Boolean = false) : Listenable, Configurable(name) {

    var state: Boolean = defaultState
        set(value) {
            field = value
            toggled()
        }

    protected val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.mc

    open fun toggled() { }

    /**
     * Registers an event hook for events of type [T]
     *
     * TODO: Check on performance and memory usage
     */
    inline fun <reified T : Event> sequenceHandler(ignoreCondition: Boolean = false, noinline eventHandler: SuspendableHandler<T>) {
        handler<T>(ignoreCondition) { event -> Sequence(eventHandler, event) }
    }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state

}
