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

import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.math.center
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

/**
 * Walks to the closest of Fucker's targets while none is in range.
 */
object FuckerAutoWalk : NavigationBaseValueGroup<Unit>(
    ModuleFucker,
    "AutoWalk",
    enabled = false,
    rotations = ModuleFucker.rotations,
    allowBreak = true,
    allowPlace = true,
) {

    private val targetRange by float("TargetRange", 64f, 8f..256f)

    override fun onEnabled() {
        ChunkScanner.subscribe(TargetTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(TargetTracker)
        super.onDisabled()
    }

    /**
     * For when Fucker's target list changed.
     */
    fun rescan() {
        if (running) {
            ChunkScanner.unsubscribe(TargetTracker)
            ChunkScanner.subscribe(TargetTracker)
        }
    }

    override fun createNavigationContext() = Unit

    override fun calculateGoalPosition(context: Unit): Vec3? {
        if (ModuleFucker.hasTarget) {
            return null
        }

        val position = player.position()
        val maxDistanceSq = targetRange.sq().toDouble()
        return TargetTracker.allPositions()
            .filter { pos -> pos.state?.let { ModuleFucker.isTarget(pos, it) } == true }
            .map { it.center }
            .filter { it.distanceToSqr(position) <= maxDistanceSq }
            .minByOrNull { it.distanceToSqr(position) }
    }

    override fun goalRadius(context: Unit) = (ModuleFucker.range - 1.0).coerceAtLeast(1.0)

    private object TargetTracker : AbstractBlockLocationTracker.BlockPos2State<Block>(), Predicate<BlockState> {
        override val shouldCallRecordBlockOnChunkUpdate: Boolean
            get() = false

        override fun chunkUpdate(chunk: LevelChunk) {
            chunk.findBlocks(this) { pos, state ->
                track(pos, state.block)
            }
        }

        override fun getStateFor(pos: BlockPos, state: BlockState): Block? =
            if (test(state)) state.block else null

        override fun test(state: BlockState) = !state.isAir && ModuleFucker.isTargetBlock(state)
    }

}
