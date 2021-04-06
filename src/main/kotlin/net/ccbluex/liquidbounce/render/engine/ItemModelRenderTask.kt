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

package net.ccbluex.liquidbounce.render.engine

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNametags
import net.ccbluex.liquidbounce.render.engine.utils.popMVP
import net.ccbluex.liquidbounce.render.engine.utils.pushMVP
import net.ccbluex.liquidbounce.utils.Mat4
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11.GL_ALPHA_TEST
import org.lwjgl.opengl.GL11.glEnable

/**
 * Renders an [ItemStack] to the screen. Primarily used by [ModuleNametags]
 */
class ItemModelRenderTask(val stack: ItemStack, val x: Int, val y: Int) : RenderTask() {
    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(level: OpenGLLevel, mvpMatrix: Mat4) {
        mc.itemRenderer.zOffset = -147F

        pushMVP(mvpMatrix)
    }

    override fun draw(level: OpenGLLevel) {
        mc.itemRenderer.renderInGui(stack, x, y)
    }

    override fun cleanupRendering(level: OpenGLLevel) {
        popMVP()

        glEnable(GL_ALPHA_TEST)
    }
}
