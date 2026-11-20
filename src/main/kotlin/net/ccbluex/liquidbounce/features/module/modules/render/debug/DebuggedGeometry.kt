/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render.debug

import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

fun interface DebuggedGeometry {
    context(env: WorldRenderEnvironment)
    fun render()
}

class DebuggedLine(private val line: Line, val color: Color4b) : DebuggedGeometry {
    val from: Vec3
    val to: Vec3

    init {
        val normalizedDirection = line.direction.normalize()

        this.from = line.position.fma(-100.0, normalizedDirection)
        this.to = line.position.fma(100.0, normalizedDirection)
    }

    context(env: WorldRenderEnvironment)
    override fun render() {
        env.withPositionRelativeToCamera {
            env.drawLine(
                from,
                to,
                color.argb,
            )
        }
    }
}

class DebuggedTriangle(
    val p1: Vec3,
    val p2: Vec3,
    val p3: Vec3,
    val color: Color4b,
) : DebuggedGeometry {
    context(env: WorldRenderEnvironment)
    override fun render() {
        env.withPositionRelativeToCamera {
            env.drawTriangle(
                p1 = p1.toVec3f(),
                p2 = p2.toVec3f(),
                p3 = p3.toVec3f(),
                argb = color.argb,
            )
        }
    }
}

class DebuggedLineSegment(val from: Vec3, val to: Vec3, val color: Color4b) : DebuggedGeometry {
    context(env: WorldRenderEnvironment)
    override fun render() {
        env.withPositionRelativeToCamera {
            env.drawLine(
                from,
                to,
                color.argb,
            )
        }
    }
}

open class DebuggedBox(val box: AABB, val color: Color4b) : DebuggedGeometry {
    context(env: WorldRenderEnvironment)
    override fun render() {
        env.drawBox(box.move(env.camera.position().reverse()), color)
    }
}

class DebuggedPoint(point: Vec3, color: Color4b, size: Double = 0.2) : DebuggedBox(
    AABB.ofSize(point, size, size, size),
    color
)

class DebugCollection(val geometry: Collection<DebuggedGeometry>) : DebuggedGeometry {
    context(env: WorldRenderEnvironment)
    override fun render() {
        this.geometry.forEach { it.render() }
    }
}
