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

package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.matrix

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.sq

/**
 * bypassing matrix version > 7
 * testing in 6/23/25 at loyisa
 *
 * @author XeContrast
 */
class SpeedMatrix7(parent : ModeValueGroup<*>) : SpeedBHopBase("Matrix7",parent) {

    @Suppress("unused")
    private val tickHandle = tickHandler {
        if (player.moving) {
            if (player.onGround()) {
                player.deltaMovement.y = 0.419652
                player.deltaMovement = player.deltaMovement.withStrafe()
            } else {
                if (player.deltaMovement.x.sq() + player.deltaMovement.z.sq() < 0.04) {
                    player.deltaMovement = player.deltaMovement.withStrafe()
                }
            }
        }
    }
}
