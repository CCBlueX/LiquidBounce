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
package net.ccbluex.liquidbounce.utils.block.liquid

import net.minecraft.world.level.block.LiquidBlockContainer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids

/** Matches the non-container branch of Minecraft 26.2 BucketItem.emptyContents(). */
internal fun BlockState.canPlaceStandaloneFluid(fluid: Fluid): Boolean {
    return block !is LiquidBlockContainer && (isAir || canBeReplaced(fluid))
}

/** Water buckets need sneak to skip LiquidBlockContainer.useItemOn and place adjacent water. */
internal fun BlockState.requiresSneakForAdjacentFluidPlacement(fluid: Fluid): Boolean {
    return fluid == Fluids.WATER && block is LiquidBlockContainer
}
