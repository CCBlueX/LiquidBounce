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

package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleProjectileAimbot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.utils.findVisiblePointFromVirtualEye
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.text.DecimalFormat

/**
 * Utility class which finds a visible (= hittable) point on the target.
 */
object ProjectileTargetPointFinder {
    fun findHittablePosition(
        playerHeadPosition: Vec3d,
        directionOnImpact: Vec3d,
        entityPositionOnImpact: Vec3d,
        targetEntityBox: Box
    ): Vec3d? {
        val virtualEyes = playerHeadPosition.add(
            0.0,
            directionOnImpact.y * -(playerHeadPosition.distanceTo(entityPositionOnImpact)),
            0.0
        )
        val currTime = System.nanoTime()

        val bestPos = findVisiblePointFromVirtualEye(virtualEyes, targetEntityBox, 5.0) ?: run {
            logRaytraceTime(currTime)
            return null
        }

        logRaytraceTime(currTime)
        return bestPos
    }

    private fun logRaytraceTime(currTime: Long) {
        val deltaNanos = System.nanoTime() - currTime

        val formattedNumber = DecimalFormat("0.00").format(deltaNanos / 1E6)

        ModuleDebug.debugParameter(ModuleProjectileAimbot, "raytraceTime", "${formattedNumber}us")
    }
}
