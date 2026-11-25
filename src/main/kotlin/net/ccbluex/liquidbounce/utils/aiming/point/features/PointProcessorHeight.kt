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
 * of following every jump, with the view near level: within [pitch], which in recorded fights is where players hold
 * it. The view only turns up or down once it leaves that, or the height gets closer to an edge than [margin] of the
 * box's height.
 */
internal class PointProcessorHeight(parent: EventListener) : PointProcessor(parent, "KeepHeight", true) {

    private val pitch by floatRange("Pitch", 0f..10f, -90f..90f, "°")
    private val margin by float("Margin", 0.05f, 0f..0.5f)

    override fun process(point: PointInsideBox, eyes: Vec3): PointInsideBox {
        val view = RotationManager.currentRotation ?: player.rotation
        val box = point.box
        val inset = (box.maxY - box.minY) * margin
        val distance = hypot(point.x() - eyes.x, point.z() - eyes.z)
        // Without the pitch limits the view stays wherever a jump pushed it, which after the player's own is the feet
        val kept = view.pitch.coerceIn(pitch.start, pitch.endInclusive)
        val height = eyes.y - tan(Math.toRadians(kept.toDouble())) * distance
        // The box comes last: the view has to meet it, also where that is off the pitch it would keep
        return PointInsideBox(Vec3(point.x(), height.coerceIn(box.minY + inset, box.maxY - inset), point.z()), box)
    }

}
