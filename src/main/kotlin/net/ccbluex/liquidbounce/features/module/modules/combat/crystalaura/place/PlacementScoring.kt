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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_UP
import net.ccbluex.liquidbounce.utils.block.isBlastResistant
import net.ccbluex.liquidbounce.utils.math.topCenter
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * Bonuses and penalties on top of the enemy damage when choosing where to place, in damage points.
 * All weights default to zero, which leaves the choice to the damage alone.
 */
object PlacementScoring : ValueGroup("Scoring") {

    /**
     * Penalty for the full 180° yaw change a placement needs.
     */
    private val yawChange by float("YawChange", 0f, 0f..10f)

    /**
     * Bonus for crystals placed into a 1x1 hole, which also takes that hole away from the target.
     */
    private val oneByOneHole by float("OneByOneHole", 0f, 0f..10f)

    fun bonus(pos: BlockPos, eyePos: Vec3): Float {
        var bonus = 0f

        if (oneByOneHole > 0f && pos.above().isOneByOneHole()) {
            bonus += oneByOneHole
        }

        if (yawChange > 0f) {
            val yaw = Rotation.lookingAt(pos.topCenter, eyePos).yaw
            bonus -= yawChange * abs(Mth.wrapDegrees(yaw - RotationManager.serverRotation.yaw)) / 180f
        }

        return bonus
    }

    /**
     * A spot enclosed by blast-resistant blocks on all sides and below.
     */
    private fun BlockPos.isOneByOneHole() = DIRECTIONS_EXCLUDING_UP.all { relative(it).isBlastResistant() }

}
