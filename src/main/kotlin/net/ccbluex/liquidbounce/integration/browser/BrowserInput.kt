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
package net.ccbluex.liquidbounce.integration.browser

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.integration.browser.supports.IBrowser
import net.ccbluex.liquidbounce.integration.browser.supports.tab.InputAware
import org.lwjgl.glfw.GLFW

class BrowserInput(val browser: () -> IBrowser?) : Listenable {

    private val tabs
        get() = browser()?.getTabs() ?: emptyList()

    private var mouseX: Double = 0.0
    private var mouseY: Double = 0.0

    @Suppress("unused")
    val mouseButtonHandler = handler<MouseButtonEvent> {
        for (tab in tabs) {
            if (tab !is InputAware || !tab.takesInput()) {
                continue
            }

            if (it.action == GLFW.GLFW_PRESS) {
                tab.mouseClicked(tab.position.x(mouseX), tab.position.y(mouseY),
                    it.button)
            } else if (it.action == GLFW.GLFW_RELEASE) {
                tab.mouseReleased(tab.position.x(mouseX), tab.position.y(mouseY),
                    it.button)
            }
        }
    }

    @Suppress("unused")
    val mouseScrollHandler = handler<MouseScrollEvent> {
        for (tab in tabs) {
            if (tab !is InputAware || !tab.takesInput()) {
                continue
            }

            tab.mouseScrolled(tab.position.x(mouseX), tab.position.y(mouseY), it.vertical)
        }
    }

    @Suppress("unused")
    val mouseCursorHandler = handler<MouseCursorEvent> { event ->
        val factorW = mc.window.framebufferWidth.toDouble() / mc.window.width.toDouble()
        val factorV = mc.window.framebufferHeight.toDouble() / mc.window.height.toDouble()
        val mouseX = event.x * factorW
        val mouseY = event.y * factorV

        for (tab in tabs) {
            if (tab !is InputAware || !tab.takesInput()) {
                continue
            }

            tab.mouseMoved(tab.position.x(mouseX), tab.position.y(mouseY))
        }

        this.mouseX = mouseX
        this.mouseY = mouseY
    }

    @Suppress("unused")
    val keyboardKeyHandler = handler<KeyboardKeyEvent> {
        val action = it.action
        val key = it.keyCode
        val scancode = it.scanCode
        val modifiers = it.mods

        for (tab in tabs) {
            if (tab !is InputAware || !tab.takesInput()) {
                continue
            }

            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                tab.keyPressed(key, scancode, modifiers)
            } else if (action == GLFW.GLFW_RELEASE) {
                tab.keyReleased(key, scancode, modifiers)
            }
        }
    }

    @Suppress("unused")
    val keyboardCharHandler = handler<KeyboardCharEvent> { ev ->
        for (tab in tabs) {
            if (tab !is InputAware || !tab.takesInput()) {
                continue
            }

            tab.charTyped(ev.codePoint.toChar(), ev.modifiers)
        }
    }

}
