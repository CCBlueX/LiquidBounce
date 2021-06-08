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

import net.ccbluex.liquidbounce.utils.math.Mat4
import org.lwjgl.opengl.GL11

enum class CullingMode(val face: Int?, val enableCulling: Boolean) {
    BACKFACE_CULLING(GL11.GL_BACK, true),
    FRONTFACE_CULLING(GL11.GL_FRONT_FACE, true),
    NO_CULLING(null, false);

    fun applyState() {
        applyCap(GL11.GL_CULL_FACE, this.enableCulling)

        if (this.enableCulling) {
            GL11.glCullFace(this.face!!)
        }
    }
}



data class GlRenderState(
    val lineWidth: Float? = null,
    val depthTest: Boolean? = null,
    val lineSmooth: Boolean? = null,
    val texture2d: Boolean? = null,
    val blending: Boolean? = null,
    val culling: CullingMode? = null,
    val mvpMatrix: Mat4? = null,
) {
    fun applyFlags() {
        this.lineWidth?.let(GL11::glLineWidth)

        applyCap(GL11.GL_DEPTH_TEST, this.depthTest)
        applyCap(GL11.GL_LINE_SMOOTH, this.lineSmooth)
        applyCap(GL11.GL_BLEND, this.blending)
        applyCap(GL11.GL_TEXTURE_2D, this.texture2d)

        this.culling?.applyState()
    }
}

private fun applyCap(cap: Int, enabled: Boolean?) {
    if (enabled == null) {
        return
    }

    if (GL11.glIsEnabled(cap) == enabled) {
        return
    }

    if (enabled) {
        GL11.glEnable(cap)
    } else {
        GL11.glDisable(cap)
    }
}
