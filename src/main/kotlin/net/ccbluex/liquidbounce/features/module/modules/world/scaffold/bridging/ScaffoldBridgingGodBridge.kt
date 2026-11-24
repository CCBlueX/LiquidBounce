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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.sin

/**
 * Holds the look a god bridging player holds the mouse at: back against the movement and 45 degrees towards the middle
 * of the row, or straight back along a diagonal, steeply down. It walks over the edges.
 */
object ScaffoldBridgingGodBridge : ScaffoldBridging("GodBridge", "Ledge") {

    /** 1 or -1: the side of straight back the look turns to. */
    private var side = 1f

    override val shouldHoldLook: Boolean
        get() = true

    fun reset() {
        side = 1f
    }

    override fun look(position: Vec3): Rotation? {
        val movement = movement() ?: return null
        // The walking player meets the face of the row on the last step before leaving it, mostly
        val pitch = 75.5f
        if (movement % 90f != 0f) {
            return Rotation(movement + 180f, pitch)
        }

        // How far the player stands right of the row's middle, seen along the movement
        val radians = Math.toRadians(movement.toDouble())
        val right = -(position.x - floor(position.x) - 0.5) * cos(radians) -
            (position.z - floor(position.z) - 0.5) * sin(radians)
        // Until 0.15 blocks off the middle the look still meets the face, and StabilizeMovement may overshoot a little
        if (abs(right) > 0.15) {
            side = sign(right).toFloat()
        }
        // Looking back, the middle of a player standing right of it is on the right, where yaw grows
        return Rotation(movement + 180f + side * 45f, pitch)
    }

}
