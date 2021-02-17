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

package net.ccbluex.liquidbounce.renderer.engine.utils

import net.ccbluex.liquidbounce.renderer.engine.Color4b
import net.ccbluex.liquidbounce.renderer.engine.ColoredPrimitiveRenderTask
import net.ccbluex.liquidbounce.renderer.engine.PrimitiveType
import net.ccbluex.liquidbounce.renderer.engine.Vec3
import net.minecraft.util.math.Box

fun drawBox(box: Box, color: Color4b): ColoredPrimitiveRenderTask {
    val renderTask = ColoredPrimitiveRenderTask(12, PrimitiveType.Triangles)

    val p0 = renderTask.vertex(Vec3(box.minX, box.minY, box.maxZ), color)
    val p1 = renderTask.vertex(Vec3(box.maxX, box.minY, box.maxZ), color)
    val p2 = renderTask.vertex(Vec3(box.minX, box.maxY, box.maxZ), color)
    val p3 = renderTask.vertex(Vec3(box.maxX, box.maxY, box.maxZ), color)

    val p4 = renderTask.vertex(Vec3(box.minX, box.minY, box.minZ), color)
    val p5 = renderTask.vertex(Vec3(box.maxX, box.minY, box.minZ), color)
    val p6 = renderTask.vertex(Vec3(box.minX, box.maxY, box.minZ), color)
    val p7 = renderTask.vertex(Vec3(box.maxX, box.maxY, box.minZ), color)

    renderTask.indexQuad(p2, p0, p1, p3)
    renderTask.indexQuad(p6, p4, p0, p2)
    renderTask.indexQuad(p7, p5, p4, p6)
    renderTask.indexQuad(p3, p1, p5, p7)
    renderTask.indexQuad(p6, p2, p3, p7)
    renderTask.indexQuad(p0, p4, p5, p1)

    return renderTask
}

fun drawBoxOutline(box: Box, color: Color4b): ColoredPrimitiveRenderTask {
    val renderTask = ColoredPrimitiveRenderTask(12, PrimitiveType.Lines)

    val p0 = renderTask.vertex(Vec3(box.minX, box.minY, box.maxZ), color)
    val p1 = renderTask.vertex(Vec3(box.maxX, box.minY, box.maxZ), color)
    val p2 = renderTask.vertex(Vec3(box.minX, box.maxY, box.maxZ), color)
    val p3 = renderTask.vertex(Vec3(box.maxX, box.maxY, box.maxZ), color)

    val p4 = renderTask.vertex(Vec3(box.minX, box.minY, box.minZ), color)
    val p5 = renderTask.vertex(Vec3(box.maxX, box.minY, box.minZ), color)
    val p6 = renderTask.vertex(Vec3(box.minX, box.maxY, box.minZ), color)
    val p7 = renderTask.vertex(Vec3(box.maxX, box.maxY, box.minZ), color)

    renderTask.indexLine(p0, p1)
    renderTask.indexLine(p2, p3)
    renderTask.indexLine(p4, p5)
    renderTask.indexLine(p6, p7)

    renderTask.indexLine(p0, p4)
    renderTask.indexLine(p1, p5)
    renderTask.indexLine(p2, p6)
    renderTask.indexLine(p3, p7)

    renderTask.indexLine(p0, p2)
    renderTask.indexLine(p1, p3)
    renderTask.indexLine(p4, p6)
    renderTask.indexLine(p5, p7)


    return renderTask
}

fun ColoredPrimitiveRenderTask.indexQuad(p0: Int, p1: Int, p2: Int, p3: Int) {
    index(p0)
    index(p1)
    index(p3)

    index(p1)
    index(p2)
    index(p3)
}

fun ColoredPrimitiveRenderTask.indexLine(p0: Int, p1: Int) {
    index(p0)
    index(p1)
}

/**
 * Draws a rect along the X and Y axis
 */
fun ColoredPrimitiveRenderTask.rect(p1: Vec3, p2: Vec3, color: Color4b, outline: Boolean = false) {
    if (outline) {
        this.outlineQuad(
            Vec3(p1.x, p1.y, p1.z),
            Vec3(p1.x, p2.y, p1.z),
            Vec3(p2.x, p2.y, p1.z),
            Vec3(p2.x, p1.y, p1.z),
            color
        )
    } else {
        this.quad(
            Vec3(p1.x, p1.y, p1.z),
            Vec3(p1.x, p2.y, p1.z),
            Vec3(p2.x, p2.y, p1.z),
            Vec3(p2.x, p1.y, p1.z),
            color
        )
    }
}
