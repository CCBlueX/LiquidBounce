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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.InteractionHand

object NormalMineMode : MineMode("Normal") {

    private val clientSideSet by boolean("ClientSideSet", false)
    private val waitForConfirm by boolean("WaitForConfirm", true)

    override fun start(mineTarget: MineTarget) {
        EventManager.callEvent(BlockBreakingProgressEvent(mineTarget.targetPos))
        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                mineTarget.targetPos,
                mineTarget.direction!!,
                sequence,
            )
        }

        ModulePacketMine.swingMode.swing(InteractionHand.MAIN_HAND)
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

        if (clientSideSet) {
            interaction.destroyBlock(mineTarget.targetPos)
        }

        mineTarget.finished = true
        if (!waitForConfirm) {
            ModulePacketMine._resetTarget()
        }
    }

    override fun shouldPreventTargetChange(mineTarget: MineTarget): Boolean {
        return waitForConfirm && mineTarget.finished
    }

    override fun shouldUpdate(mineTarget: MineTarget, slot: HotbarItemSlot?): Boolean {
        return !mineTarget.finished
    }

}
