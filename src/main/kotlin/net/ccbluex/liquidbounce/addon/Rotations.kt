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
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction

/**
 * Server-side rotations, shared with every module of the client. The highest priority request wins.
 */
object Rotations {

    /** Roughly where the client's own combat and placement modules rank. */
    const val PRIORITY_NORMAL = 0
    const val PRIORITY_COMBAT = 30
    const val PRIORITY_PLACEMENT = 40

    @JvmStatic
    val serverYaw: Float
        get() = RotationManager.serverRotation.yaw

    @JvmStatic
    val serverPitch: Float
        get() = RotationManager.serverRotation.pitch

    /**
     * Sends [yaw]/[pitch] to the server for [holdTicks] ticks without turning the camera. The request
     * ends early when [owner] stops running. [onReached] runs once the rotation has been applied.
     */
    @JvmStatic
    @JvmOverloads
    fun request(
        owner: ClientModule,
        yaw: Float,
        pitch: Float,
        priority: Int = PRIORITY_NORMAL,
        holdTicks: Int = 1,
        onReached: Runnable? = null,
    ) {
        RotationManager.setRotationTarget(
            RotationTarget(
                rotation = Rotation(yaw, pitch),
                ticksUntilReset = holdTicks,
                resetThreshold = 1f,
                considerInventory = false,
                movementCorrection = MovementCorrection.OFF,
                whenReached = onReached?.let { RestrictedSingleUseAction({ true }, it) },
            ),
            priority,
            owner,
        )
    }

}
