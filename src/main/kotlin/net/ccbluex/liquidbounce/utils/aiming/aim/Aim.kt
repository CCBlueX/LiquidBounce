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
package net.ccbluex.liquidbounce.utils.aiming.aim

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta

/**
 * How the view turns towards a goal. The result is rounded to whole mouse counts afterwards, so the view only ever
 * takes steps a mouse could make.
 */
@AddonApi
abstract class Aim(
    name: String,
    override val parent: ModeValueGroup<*>,
    aliases: List<String> = emptyList(),
) : Mode(name, aliases) {

    /**
     * Degrees to turn from [view] this tick. [fresh] is true when this aim did not turn the view in the tick before,
     * so whatever it was doing is over.
     */
    abstract fun turn(view: Rotation, goal: AimGoal, fresh: Boolean): RotationDelta

    /** Ticks after this one it takes at least to turn from [from] to [to], 0 if this tick's turn gets there. */
    abstract fun ticksToReach(from: Rotation, to: Rotation): Int

}

/**
 * Where the view has to go: [rotation] is the point to aim at, [width] and [height] the angular size of what has to be
 * hit (0 for the rotation itself), and [reached] whether the view hits it already.
 */
@AddonApi
class AimGoal(val rotation: Rotation, val width: Float, val height: Float, val reached: Boolean)
