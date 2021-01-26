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
import net.ccbluex.liquidbounce.renderer.engine.Point3f
import net.ccbluex.liquidbounce.renderer.engine.PrimitiveType
import net.minecraft.util.math.Box

fun drawBox(box: Box, color: Color4b): ColoredPrimitiveRenderTask {
    val renderTask = ColoredPrimitiveRenderTask(12, PrimitiveType.Triangles)

    val p0 = renderTask.vertex(Point3f(box.minX, box.minY, box.maxZ), color)
    val p1 = renderTask.vertex(Point3f(box.maxX, box.minY, box.maxZ), color)
    val p2 = renderTask.vertex(Point3f(box.minX, box.maxY, box.maxZ), color)
    val p3 = renderTask.vertex(Point3f(box.maxX, box.maxY, box.maxZ), color)

    val p4 = renderTask.vertex(Point3f(box.minX, box.minY, box.minZ), color)
    val p5 = renderTask.vertex(Point3f(box.maxX, box.minY, box.minZ), color)
    val p6 = renderTask.vertex(Point3f(box.minX, box.maxY, box.minZ), color)
    val p7 = renderTask.vertex(Point3f(box.maxX, box.maxY, box.minZ), color)

    renderTask.indexQuad(p2, p0, p1, p3)
    renderTask.indexQuad(p6, p4, p0, p2)
    renderTask.indexQuad(p7, p5, p4, p6)
    renderTask.indexQuad(p3, p1, p5, p7)
    renderTask.indexQuad(p6, p2, p3, p7)
    renderTask.indexQuad(p0, p4, p5, p1)

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
