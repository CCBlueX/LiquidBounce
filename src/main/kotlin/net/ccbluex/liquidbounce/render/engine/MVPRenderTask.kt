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

package net.ccbluex.liquidbounce.render.engine

import net.ccbluex.liquidbounce.utils.math.Mat4

/**
 * A collection of render tasks that use an own MVP matrix besides the current MVP matrix. Can only be used for one type of render task.
 * The first task's initialize and cleanup methods are used for *all* tasks. The task has to be compatible with that
 *
 *
 * **Tested Compatible Tasks:**
 * - [ItemModelRenderTask]
 *
 * Other tasks might work too.
 *
 * @param matrix The matrix that will be multiplied with the base MVP matrix to get the final MVP matrix
 */
class MVPRenderTask(val renderTask: Array<RenderTask>, private val matrix: Mat4) : RenderTask() {
    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(level: OpenGLLevel, mvpMatrix: Mat4) {
        val mvp = Mat4(mvpMatrix)

        mvp.multiply(this.matrix)

        this.renderTask.first().initRendering(level, mvp)
    }

    override fun draw(level: OpenGLLevel) = renderTask.forEach { it.draw(level) }

    override fun cleanupRendering(level: OpenGLLevel) = this.renderTask.first().cleanupRendering(level)

}
