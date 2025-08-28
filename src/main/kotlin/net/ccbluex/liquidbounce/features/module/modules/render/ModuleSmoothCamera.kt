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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

/**
 * SmoothCamera module
 *
 * Makes your camera move smoother.
 */
object ModuleSmoothCamera : ClientModule("SmoothCamera", Category.RENDER) {

    private val factor by float("Factor", 0.2f, 0.0f..1.0f)

    var smoothPos: Vec3d = Vec3d.ZERO
        private set
    var smoothYaw = 0f
        private set
    var smoothPitch = 0f
        private set

    override fun onDisabled() {
        smoothPos = Vec3d.ZERO
        smoothYaw = 0f
        smoothPitch = 0f
    }

    @JvmStatic
    fun cameraUpdate(yaw: Float, pitch: Float, pos: Vec3d) {
        if (!running) return

        if (smoothPos.isLikelyZero) {
            smoothPos = pos
            smoothYaw = yaw
            smoothPitch = pitch
        }

        val eased = factor

        smoothPos = smoothPos.lerp(pos, eased.toDouble())
        smoothYaw += MathHelper.wrapDegrees(yaw - smoothYaw) * eased
        smoothPitch += (pitch - smoothPitch) * eased
    }

    @JvmStatic
    fun shouldApplyChanges(): Boolean = running

}
