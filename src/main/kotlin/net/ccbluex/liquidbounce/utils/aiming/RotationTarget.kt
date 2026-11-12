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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.aiming.aim.Aim
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB

/**
 * A rotation a module wants the view to turn to.
 */
@AddonApi
@Suppress("LongParameterList")
class RotationTarget @JvmOverloads constructor(
    val rotation: Rotation,
    val entity: Entity? = null,
    /**
     * How the view turns towards [rotation]; null turns it at once.
     */
    val aim: Aim? = null,
    /**
     * Ticks the target stays after the module stopped asking for it, before the view turns back to the player's.
     */
    val ticksUntilReset: Int = 1,
    /**
     * Holds the view while the inventory is open.
     */
    val considerInventory: Boolean = false,
    val movementCorrection: MovementCorrection = MovementCorrection.SILENT,
    val whenReached: RestrictedSingleUseAction? = null,
    /**
     * The axes this target turns. The others stay with the player's own view.
     */
    val axes: RotationAxes = RotationAxes.BOTH,
    /**
     * What the view has to point at: any rotation whose ray hits it is on target. Defaults to the box of [entity].
     */
    val box: AABB? = null,
    /**
     * Degrees [rotation] may be missed by on each turned axis, when there is no [box].
     */
    val tolerance: Float = 0f,
) {

    /** The box to hit, if there is one. */
    val hitBox: AABB?
        get() = box ?: entity?.let { entity -> entity.boundingBox.inflate(entity.pickRadius.toDouble()) }

}

@AddonApi
enum class RotationAxes(val yaw: Boolean, val pitch: Boolean) {
    BOTH(true, true),
    YAW(true, false),
    PITCH(false, true),
}
