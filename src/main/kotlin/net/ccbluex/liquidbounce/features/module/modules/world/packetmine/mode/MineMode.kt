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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.block.isBreakable
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

abstract class MineMode(
    name: String,
    val canManuallyChange: Boolean = true,
    val canAbort: Boolean = true,
    val stopOnStateChange: Boolean = true
) : Mode(name) {

    open fun isInvalid(mineTarget: MineTarget, state: BlockState): Boolean {
        return state.isNotBreakable(mineTarget.targetPos) && !player.isCreative || state.isAir
    }

    open fun shouldTarget(blockPos: BlockPos, state: BlockState): Boolean {
        return state.isBreakable(blockPos)
    }

    open fun onCannotLookAtTarget(mineTarget: MineTarget) {}

    abstract fun start(mineTarget: MineTarget)

    abstract fun finish(mineTarget: MineTarget)

    abstract fun shouldUpdate(
        mineTarget: MineTarget,
        slot: IntObjectImmutablePair<ItemStack>?
    ): Boolean

    override val parent: ModeValueGroup<*>
        get() = ModulePacketMine.mode

}
