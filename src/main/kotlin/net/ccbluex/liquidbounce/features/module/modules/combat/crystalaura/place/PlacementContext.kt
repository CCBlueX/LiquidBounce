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

import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class PlacementContext(
    val basePlace: Boolean,
    val basePlaceLayers: IntRange,
    val expectedCrystal: AABB,
    val target: LivingEntity
) {

    val eyePos: Vec3 = player.eyePosition
    val range = SubmoduleCrystalPlacer.range.toDouble()
    val wallsRange = SubmoduleCrystalPlacer.wallsRange.toDouble()

}

class CandidateCache(private val candidate: BlockPos) {

    val state by lazy {
        candidate.getState()!!
    }

    val canPlace by lazy {
        state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK
    }

    val up: BlockPos by lazy {
        candidate.above()
    }

}

fun interface PlacementCondition {
    fun isValid(context: PlacementContext, cache: CandidateCache, candidate: BlockPos): Boolean
}
