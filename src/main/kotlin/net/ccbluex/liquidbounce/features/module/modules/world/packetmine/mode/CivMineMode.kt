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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool.findBestToolToMineBlock
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object CivMineMode : MineMode("Civ", stopOnStateChange = false) {

    private val switch by boolean("Switch", false)

    override fun isInvalid(mineTarget: MineTarget, state: BlockState): Boolean {
        return state.getHardness(world, mineTarget.targetPos) == 1f && !player.isCreative
    }

    override fun onCannotLookAtTarget(mineTarget: MineTarget) {
        // send always a packet to keep the target
        network.sendPacket(PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            mineTarget.targetPos,
            Direction.DOWN
        ))
    }

    override fun shouldTarget(blockPos: BlockPos, state: BlockState): Boolean {
        return state.getHardness(world, blockPos) > 0f
    }

    override fun start(mineTarget: MineTarget) {
        NormalMineMode.start(mineTarget)
    }

    override fun finish(mineTarget: MineTarget) {
        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                mineTarget.targetPos,
                mineTarget.direction
            )
        )

        ModulePacketMine.swingMode.swing(Hand.MAIN_HAND)

        mineTarget.finished = true
    }

    override fun shouldUpdate(mineTarget: MineTarget, slot: IntObjectImmutablePair<ItemStack>?): Boolean {
        if (!mineTarget.finished) {
            return true
        }

        // some blocks only break when holding a certain tool
        val oldSlot = player.inventory.selectedSlot
        val state = world.getBlockState(mineTarget.targetPos)
        var shouldSwitch = switch && state.isToolRequired
        if (shouldSwitch && ModuleAutoTool.running) {
            ModuleAutoTool.switchToBreakBlock(mineTarget.targetPos)
            shouldSwitch = false
        } else if (shouldSwitch) {
            val slot1 = Slots.Hotbar.findBestToolToMineBlock(state)?.hotbarSlot
            if (slot1 != null && slot1 != oldSlot) {
                network.sendPacket(UpdateSelectedSlotC2SPacket(slot1))
            } else {
                shouldSwitch = false
            }
        }

        // Alright, for some reason when we spam STOP_DESTROY_BLOCK
        // server accepts us to destroy the same block instantly over and over.
        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                mineTarget.targetPos,
                mineTarget.direction
            )
        )

        if (shouldSwitch) {
            network.sendPacket(UpdateSelectedSlotC2SPacket(oldSlot))
        }

        return false
    }

}
