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
package net.ccbluex.liquidbounce.features.module.modules.world.fucker

import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.math.center
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.minecraft.world.phys.Vec3

/**
 * Walks to the closest bed among the targets while nothing is in range, bridging and tunneling as needed.
 */
object FuckerAutoWalk : NavigationBaseValueGroup<Unit>(
    ModuleFucker,
    "AutoWalk",
    enabled = false,
    rotations = ModuleFucker.rotations,
    allowBreak = true,
    allowPlace = true,
), BedBlockTracker.Subscriber {

    private val bedRange by float("BedRange", 64f, 8f..256f)

    override val maxLayers: Int
        get() = 1

    override fun onEnabled() {
        BedBlockTracker.subscribe(this)
    }

    override fun onDisabled() {
        BedBlockTracker.unsubscribe(this)
        super.onDisabled()
    }

    override fun createNavigationContext() = Unit

    override fun calculateGoalPosition(context: Unit): Vec3? {
        if (ModuleFucker.hasTarget) {
            return null
        }

        val position = player.position()
        val maxDistanceSq = bedRange.sq().toDouble()
        return BedBlockTracker.allPositions()
            .filter { pos -> pos.state?.let { ModuleFucker.isTarget(pos, it) } == true }
            .map { it.center }
            .filter { it.distanceToSqr(position) <= maxDistanceSq }
            .minByOrNull { it.distanceToSqr(position) }
    }

    override fun goalRadius(context: Unit) = (ModuleFucker.range - 1.0).coerceAtLeast(1.0)

}
