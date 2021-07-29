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
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.Mat4
import net.minecraft.item.ItemStack

/**
 * Renders an [ItemStack] to the screen. Primarily used by [ModuleNametags]
 */
class ItemModelRenderTask(val stack: ItemStack, val x: Int, val y: Int) : RenderTask() {
    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(level: OpenGLLevel, mvpMatrix: Mat4) {
        mc.itemRenderer.zOffset = -147F

//        pushMVP(mvpMatrix)
    }

    override fun draw(level: OpenGLLevel) {
        mc.itemRenderer.renderInGui(stack, x, y)
    }

    override fun cleanupRendering(level: OpenGLLevel) {
//        popMVP()
    }
}
