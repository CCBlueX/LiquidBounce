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
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.minecraft.world.entity.Entity

/**
 * What a module wants the view to do. An axis left null is up to the rotation system: it stays with the player's own
 * view, or with a target that does turn it.
 */
@AddonApi
@Suppress("LongParameterList")
class RotationTarget @JvmOverloads constructor(
    val yaw: Float?,
    val pitch: Float?,
    val entity: Entity? = null,
    /**
     * How the view turns; null turns it at once.
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
    /**
     * What the view has to meet, with [yaw] and [pitch] as the point on it the module recommends. Defaults to the box
     * of [entity].
     */
    val surface: Surface? = null,
    /**
     * Degrees [yaw] and [pitch] may be missed by, when there is no [surface].
     */
    val tolerance: Float = 0f,
    /**
     * Told on the tick the rotation going out meets what the module needs, which is when vanilla acts on a look too.
     */
    val whenReady: Readiness<*>? = null,
) {

    init {
        require(yaw != null || pitch != null) { "A rotation target has to turn at least one axis" }
    }

    @JvmOverloads
    constructor(
        rotation: Rotation,
        entity: Entity? = null,
        aim: Aim? = null,
        ticksUntilReset: Int = 1,
        considerInventory: Boolean = false,
        movementCorrection: MovementCorrection = MovementCorrection.SILENT,
        surface: Surface? = null,
        tolerance: Float = 0f,
        whenReady: Readiness<*>? = null,
    ) : this(
        rotation.yaw,
        rotation.pitch,
        entity,
        aim,
        ticksUntilReset,
        considerInventory,
        movementCorrection,
        surface,
        tolerance,
        whenReady,
    )

    /** The surface to hit, if there is one. */
    val hitSurface: Surface?
        get() = surface ?: entity?.let(Surface::of)

    /** This target's angles, with [otherwise] on an axis it leaves free. */
    fun rotation(otherwise: Rotation) = Rotation(yaw ?: otherwise.yaw, pitch ?: otherwise.pitch)

}
