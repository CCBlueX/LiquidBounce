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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.Hand

object NormalMineMode : MineMode("Normal") {

    private val clientSideSet by boolean("ClientSideSet", false)
    private val waitForConfirm by boolean("WaitForConfirm", true)

    override fun start(mineTarget: MineTarget) {
        EventManager.callEvent(BlockBreakingProgressEvent(mineTarget.targetPos))
        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                mineTarget.targetPos,
                mineTarget.direction
            )
        )

        ModulePacketMine.swingMode.swing(Hand.MAIN_HAND)
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

        if (clientSideSet) {
            interaction.breakBlock(mineTarget.targetPos)
        }

        mineTarget.finished = true
        if (!waitForConfirm) {
            ModulePacketMine._resetTarget()
        }
    }

    override fun shouldUpdate(mineTarget: MineTarget, slot: IntObjectImmutablePair<ItemStack>?): Boolean {
        return !mineTarget.finished
    }

}
