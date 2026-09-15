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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.horizontalSpeed
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.stopXZVelocity

/**
 * The player's own movement. Velocity changes take effect in the tick they are made in.
 */
object Movement {

    /** Whether a movement key is held. */
    @JvmStatic
    val isMoving: Boolean
        get() = Game.player.moving

    /** Blocks per tick, ignoring the vertical part. */
    @JvmStatic
    val horizontalSpeed: Double
        get() = Game.player.horizontalSpeed

    /**
     * Points the velocity where the movement keys point, at [speed] blocks per tick. Stops when no key
     * is held. [strength] below 1 keeps part of the current velocity.
     */
    @JvmStatic
    @JvmOverloads
    fun strafe(speed: Double = horizontalSpeed, strength: Double = 1.0) {
        val player = Game.player
        player.deltaMovement = player.deltaMovement.withStrafe(speed, strength)
    }

    @JvmStatic
    fun stopHorizontal() = Game.player.stopXZVelocity()

    /** The game speed the client's timer currently applies, 1 being normal. */
    @JvmStatic
    val timerSpeed: Float
        get() = Timer.timerSpeed

    /**
     * Speeds up or slows down the game for [ticks], unless a client module asked for something else.
     */
    @JvmStatic
    @JvmOverloads
    fun requestTimerSpeed(owner: ClientModule, speed: Float, ticks: Int = 1) =
        Timer.requestTimerSpeed(speed, Priority.NORMAL, owner, ticks)

}
