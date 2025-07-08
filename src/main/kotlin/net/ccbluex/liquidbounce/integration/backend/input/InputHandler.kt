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
package net.ccbluex.liquidbounce.integration.backend.input

/**
 * Interface for browsers that can handle input events
 */
interface InputHandler {

    /**
     * Handles mouse click events
     */
    fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int)

    /**
     * Handles mouse release events
     */
    fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int)

    /**
     * Handles mouse movement events
     */
    fun mouseMoved(mouseX: Double, mouseY: Double)

    /**
     * Handles mouse scroll events
     */
    fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double)

    /**
     * Handles key press events
     */
    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int)

    /**
     * Handles key release events
     */
    fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int)

    /**
     * Handles character typed events
     */
    fun charTyped(char: Char, modifiers: Int)
}
