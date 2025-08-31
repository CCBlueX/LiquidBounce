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

package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes

import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.NoWebMode
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.minecraft.util.math.BlockPos

/**
 * Intave needs to improve their movement checks
 * works on intave 14.8.4
 */
object NoWebIntave14 : NoWebMode("Intave14") {
    override fun handleEntityCollision(pos: BlockPos): Boolean {
        if (player.moving) {
            if (player.isOnGround) {
                if (player.age % 3 == 0) {
                    player.velocity = player.velocity.withStrafe(strength = 0.734)
                } else {
                    player.jump()
                    player.velocity = player.velocity.withStrafe(strength = 0.346)
                }
            }
        }
        return false
    }
}
