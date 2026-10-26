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
package net.ccbluex.liquidbounce.utils.aiming.data

import net.ccbluex.liquidbounce.annotations.ValueClassCandidate
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2

@ValueClassCandidate
@JvmRecord
data class RotationDelta(val deltaYaw: Float, val deltaPitch: Float) {
    fun length() = Mth.length(deltaYaw, deltaPitch)
    fun lengthSqr() = Mth.lengthSquared(deltaYaw, deltaPitch)
    fun toVec2f() = Vec2(deltaYaw, deltaPitch)
}
