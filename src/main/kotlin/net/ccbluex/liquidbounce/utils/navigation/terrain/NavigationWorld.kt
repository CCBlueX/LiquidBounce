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

package net.ccbluex.liquidbounce.utils.navigation.terrain

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState

/** The blocks a route may be planned through. Unknown blocks are never entered. */
interface NavigationWorld : BlockGetter {

    fun isKnown(x: Int, y: Int, z: Int): Boolean

    fun isInsideBorder(x: Double, z: Double): Boolean

}

/** The client world as it is now. Only for the render thread. */
class LiveWorld(private val level: ClientLevel) : NavigationWorld {

    override fun isKnown(x: Int, y: Int, z: Int) =
        y >= level.minY && y <= level.maxY && level.hasChunkAt(x, z)

    override fun isInsideBorder(x: Double, z: Double) = level.worldBorder.isWithinBounds(x, z)

    override fun getBlockEntity(pos: BlockPos): BlockEntity? = level.getBlockEntity(pos)

    override fun getBlockState(pos: BlockPos): BlockState = level.getBlockState(pos)

    override fun getFluidState(pos: BlockPos): FluidState = level.getFluidState(pos)

    override fun getHeight() = level.height

    override fun getMinY() = level.minY

}
