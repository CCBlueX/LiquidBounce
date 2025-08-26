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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

/**
 * Object used by the script API to provide an
 */
@Suppress("unused")
object ScriptBlockUtil {

    @JvmName("newBlockPos")
    fun newBlockPos(x: Int, y: Int, z: Int): BlockPos = BlockPos(x, y, z)

    @JvmName("getBlock")
    fun getBlock(blockPos: BlockPos) = blockPos.getBlock()

    @JvmName("getState")
    fun getState(blockPos: BlockPos): BlockState? = blockPos.getState()

}
