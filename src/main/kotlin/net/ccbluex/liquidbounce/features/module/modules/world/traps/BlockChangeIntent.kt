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
package net.ccbluex.liquidbounce.features.module.modules.world.traps

import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.item.Item
import net.minecraft.world.phys.BlockHitResult

class BlockChangeIntent<T>(
    val blockChangeInfo: BlockChangeInfo,
    val slot: HotbarItemSlot,
    val timing: IntentTiming,
    /**
     * Info for the planner.
     */
    val planningInfo: T,
    val provider: BlockIntentProvider<T>
) {
    fun validate(raycast: BlockHitResult): Boolean {
        return provider.validate(this, raycast)
    }

    fun onIntentFulfilled() {
        return provider.onIntentFulfilled(this)
    }
}

interface BlockIntentProvider<T> {
    fun validate(plan: BlockChangeIntent<T>, raycast: BlockHitResult): Boolean
    fun onIntentFulfilled(intent: BlockChangeIntent<T>)
}

sealed class BlockChangeInfo {
    class PlaceBlock(
        val blockPlacementTarget: BlockPlacementTarget
    ) : BlockChangeInfo()

    class InteractWithBlock(
        val itemPredicate: (Item) -> Boolean,
        val side: Direction,
        val alternativeOffsets: List<Vec3i> = listOf(Vec3i.ZERO)
    ) : BlockChangeInfo()
}

enum class IntentTiming {
    INSTANT,

    /**
     * Act during combat, but wait for a good moment (i.e. between hits, after a crit so the crit is not reset)
     */
    NEXT_PROPITIOUS_MOMENT,
}
