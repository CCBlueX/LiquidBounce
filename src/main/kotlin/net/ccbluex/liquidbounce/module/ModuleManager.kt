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
import net.ccbluex.liquidbounce.event.KeyEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.module.modules.movement.Fly
import net.ccbluex.liquidbounce.module.modules.render.HUD
import org.lwjgl.glfw.GLFW

/**
 * A fairly simple module manager
 */
class ModuleManager : Iterable<Module>, Listenable {

    val modules = mutableListOf<Module>()

    val keyHandler = LiquidBounce.eventManager.handler<KeyEvent>(this) { ev ->
        if(ev.action == GLFW.GLFW_PRESS) {
            filter { it.bind == ev.key.code }
                .forEach { it.state = !it.state }
        }
    }

    fun registerClientModules() {
        modules.add(HUD)
        modules.add(Fly)
        modules.add(Velocity)
    }

    override fun iterator() = modules.iterator()
    override fun handleEvents() = true

}
