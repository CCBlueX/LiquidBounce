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

package net.ccbluex.liquidbounce.utils.aiming.point.exempts

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.minecraft.util.math.Vec3d

internal class ExemptBestHitVector(parent: EventListener) :
    ToggleableConfigurable(parent, "ExemptBestHitVector", false), ExemptPoint {

    private val vertical by float("Vertical", 0.2f, 0.0f..1f)
    private val horizontal by float("Horizontal", 0.1f, 0.0f..1f)

    override fun predicate(context: ExemptContext, point: Vec3d) = enabled &&
        point.isWithinRangeOf(context.bestHitVector, horizontal.toDouble(), vertical.toDouble())

}
