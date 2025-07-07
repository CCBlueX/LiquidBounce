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
package net.ccbluex.liquidbounce.integration.backend.browser

import net.ccbluex.liquidbounce.integration.backend.BrowserTexture

/**
 * Browser interface for web content rendering and interaction
 */
interface Browser : AutoCloseable {

    var viewport: BrowserViewport
    var visible: Boolean
    var priority: Short

    /**
     * Current URL of the browser
     */
    var url: String

    /**
     * Current browser texture for rendering
     */
    val texture: BrowserTexture?

    /**
     * Reloads the page ignoring cache
     */
    fun forceReload()

    /**
     * Reloads the current page
     */
    fun reload()

    /**
     * Navigate forward in history
     */
    fun goForward()

    /**
     * Navigate back in history
     */
    fun goBack()

    /**
     * Updates browser dimensions and properties
     */
    fun update(width: Int = viewport.width, height: Int = viewport.height)

    /**
     * Invalidates the browser texture, forcing a redraw
     */
    fun invalidate()

    /**
     * String representation of the Browser Instance
     */
    override fun toString(): String

}
