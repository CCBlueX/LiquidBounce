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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.matrix

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe

/**
 * bypassing matrix version > 7
 * testing in 6/23/25 at loyisa
 *
 * @author XeContrast
 */
class SpeedMatrix7(override val parent : ChoiceConfigurable<*>) : SpeedBHopBase("Matrix7",parent) {

    @Suppress("unused")
    private val tickHandle = tickHandler {
        if (player.moving) {
            if (player.isOnGround) {
                player.velocity.y = 0.419652
                player.velocity = player.velocity.withStrafe()
            } else {
                if (player.velocity.x * player.velocity.x + player.velocity.z * player.velocity.z < 0.04) {
                    player.velocity = player.velocity.withStrafe()
                }
            }
        }
    }
}
