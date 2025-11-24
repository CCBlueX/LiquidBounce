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

package net.ccbluex.liquidbounce.render.buffer

import com.mojang.blaze3d.opengl.GlStateManager
import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glClearDepth
import java.io.Closeable

abstract class AbstractFramebuffer: Closeable {
    abstract val id: Int
    abstract val width: Int
    abstract val height: Int
    abstract val useDepth: Boolean

    var clearColor: Color4b = Color4b.WHITE

    abstract fun resize(width: Int, height: Int)

    open fun beginWrite(viewport: Boolean, clear: Boolean = true) {
        GlobalFramebuffer.push(this)

        if (viewport) {
            GlStateManager._viewport(0, 0, width, height)
        }

        if (clear) {
            glClearColor(
                clearColor.r / 255.0F,
                clearColor.g / 255.0F,
                clearColor.b / 255.0F,
                clearColor.a / 255.0F
            )

            if (useDepth) {
                glClearDepth(1.0)
            }

            GlStateManager._clear(GL_COLOR_BUFFER_BIT or (if (useDepth) GL_DEPTH_BUFFER_BIT else 0))
        }
    }

    open fun end() {
        GlobalFramebuffer.pop()
    }
}
