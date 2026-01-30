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

package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.ModuleNoWeb.modes
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.NoWebMode
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.minecraft.core.BlockPos

/**
 * Bypassing Vulcan anticheat (6/27/2025)
 * Bypassing Grim anticheat (7/28/2025)
 *
 * @author XeContrast
 */
object NoWebStrafe : NoWebMode("Strafe") {
    override val parent: ModeValueGroup<NoWebMode>
        get() = modes

    private val strength by float("Strength", 0.23f, 0.01f..0.8f)
    private val motionY = tree(Motion())
    private val onlyGround by boolean("OnlyOnGround", false)

    private class Motion : ToggleableValueGroup(this@NoWebStrafe, "MotionY", false) {
        val motionStrength by float("MotionYStrength", 0.6f, -2.00f..2.00f)
    }

    // TODO: make motionY more flexible based on player input
    override fun handleEntityCollision(pos: BlockPos): Boolean {
        if (player.moving) {
            if (player.onGround() || !onlyGround) {
                player.deltaMovement = player.deltaMovement.withStrafe(strength.toDouble())
            }

            if (motionY.enabled) {
                player.deltaMovement.y = motionY.motionStrength.toDouble()
            }
        }
        return false
    }
}
