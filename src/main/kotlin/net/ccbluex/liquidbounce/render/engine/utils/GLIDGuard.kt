/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.render.engine.utils

import net.ccbluex.liquidbounce.render.engine.RenderEngine

/**
 * Provides a guard for IDs provided by OpenGL which handles freeing of it
 *
 * @param deletionFunction A function that is called when the object should be deallocated
 */
abstract class GLIDGuard(val id: Int, val deletionFunction: (Int) -> Unit) {
    /**
     * Was [delete] called?
     */
    private var deleted = false

    protected fun finalize() {
        if (!deleted) {
            val id = this.id

            RenderEngine.runOnGlContext {
                deletionFunction(id)
            }
        }
    }

    fun delete() {
        if (!deleted) {
            deletionFunction(this.id)

            deleted = false
        }
    }

}
