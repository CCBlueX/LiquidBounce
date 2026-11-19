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
package net.ccbluex.liquidbounce.features.module.modules.combat.anchoraura

import net.ccbluex.liquidbounce.utils.math.center
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/** Geometry for placing a solid block between an anchor and the player's lower hitbox. */
internal object AnchorProtectionLogic {

    private const val MAX_LINE_DISTANCE = 1.25

    @JvmStatic
    fun lineDistance(pos: BlockPos, feet: Vec3, anchorCenter: Vec3): Double? {
        val delta = anchorCenter.subtract(feet)
        val lengthSq = delta.lengthSqr()
        if (lengthSq < 0.01) {
            return null
        }

        val center = pos.center
        val along = center.subtract(feet).dot(delta) / lengthSq
        if (along <= 0.0 || along >= 1.0) {
            return null
        }

        return center.distanceTo(feet.add(delta.scale(along))).takeIf { it <= MAX_LINE_DISTANCE }
    }

    @JvmStatic
    fun canProtectWithGlowstone(availableCount: Int, reserveCharge: Boolean): Boolean {
        val requiredCount = if (reserveCharge) 2 else 1
        return availableCount >= requiredCount
    }

}
