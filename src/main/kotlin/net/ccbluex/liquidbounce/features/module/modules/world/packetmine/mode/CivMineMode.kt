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
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool.findBestToolToMineBlock
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.client.sendHeldItemChange
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

object CivMineMode : MineMode("Civ", stopOnStateChange = false) {

    private val switch by boolean("Switch", false)

    override fun isInvalid(mineTarget: MineTarget, state: BlockState): Boolean {
        return state.getDestroySpeed(world, mineTarget.targetPos) == 1f && !player.isCreative
    }

    override fun onCannotLookAtTarget(mineTarget: MineTarget) {
        // send always a packet to keep the target
        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                mineTarget.targetPos,
                Direction.DOWN,
                sequence
            )
        }
    }

    override fun shouldTarget(blockPos: BlockPos, state: BlockState): Boolean {
        return state.getDestroySpeed(world, blockPos) > 0f
    }

    override fun start(mineTarget: MineTarget) {
        NormalMineMode.start(mineTarget)
    }

    override fun finish(mineTarget: MineTarget) {
        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                mineTarget.targetPos,
                mineTarget.direction!!,
                sequence,
            )
        }

        ModulePacketMine.swingMode.swing(InteractionHand.MAIN_HAND)

        mineTarget.finished = true
    }

    override fun shouldUpdate(mineTarget: MineTarget, slot: IntObjectImmutablePair<ItemStack>?): Boolean {
        if (!mineTarget.finished) {
            return true
        }

        // some blocks only break when holding a certain tool
        val oldSlot = player.inventory.selectedSlot
        val state = world.getBlockState(mineTarget.targetPos)
        var shouldSwitch = switch && state.requiresCorrectToolForDrops()
        if (shouldSwitch && ModuleAutoTool.running) {
            ModuleAutoTool.switchToBreakBlock(mineTarget.targetPos)
            shouldSwitch = false
        } else if (shouldSwitch) {
            val slot1 = Slots.Hotbar.findBestToolToMineBlock(state)?.hotbarSlot
            if (slot1 != null && slot1 != oldSlot) {
                network.sendHeldItemChange(slot1)
            } else {
                shouldSwitch = false
            }
        }

        // Alright, for some reason when we spam STOP_DESTROY_BLOCK
        // server accepts us to destroy the same block instantly over and over.
        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                mineTarget.targetPos,
                mineTarget.direction!!,
                sequence,
            )
        }

        if (shouldSwitch) {
            network.sendHeldItemChange(oldSlot)
        }

        return false
    }

}
