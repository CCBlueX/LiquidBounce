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

package net.ccbluex.liquidbounce.utils.aiming.point.features

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.point.PointInsideBox
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.hypot
import kotlin.math.tan

/**
 * Keeps the point at the height the view meets the box, as players keep their crosshair's height on a target instead
 * of following every jump. The view only turns up or down once that height gets closer to an edge than [margin] of
 * the box's height.
 */
internal class PointProcessorHeight(parent: EventListener) : PointProcessor(parent, "KeepHeight", true) {

    private val margin by float("Margin", 0.05f, 0f..0.5f)

    override fun process(point: PointInsideBox, eyes: Vec3): PointInsideBox {
        val view = RotationManager.currentRotation ?: player.rotation
        val box = point.box
        val inset = (box.maxY - box.minY) * margin
        val distance = hypot(point.x() - eyes.x, point.z() - eyes.z)
        val height = eyes.y - tan(Math.toRadians(view.pitch.toDouble())) * distance
        return PointInsideBox(Vec3(point.x(), height.coerceIn(box.minY + inset, box.maxY - inset), point.z()), box)
    }

}
