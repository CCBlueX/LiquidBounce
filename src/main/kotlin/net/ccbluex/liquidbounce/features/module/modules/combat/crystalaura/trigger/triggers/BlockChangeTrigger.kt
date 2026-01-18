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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.triggers

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.player
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.runPlace
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.PostPacketTrigger
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.phys.Vec3
import kotlin.math.max

/**
 * Runs placing right when a block was broken in the area where the aura operates.
 * This can help to block the surround of enemies with immediate placements.
 */
object BlockChangeTrigger : PostPacketTrigger<ClientboundBlockUpdatePacket>("BlockChange", true) {

    override fun postPacketHandler(packet: ClientboundBlockUpdatePacket) {
        if (!packet.blockState.isAir) {
            return
        }

        tickIfInRange(
            packet.pos,
            player.eyePosition,
            max(SubmoduleCrystalPlacer.getMaxRange(), SubmoduleCrystalDestroyer.getMaxRange()).sq() + 1.0
        )
    }

    fun postChunkUpdateHandler(packet: ClientboundSectionBlocksUpdatePacket) {
        if (!running) {
            return
        }

        val eyePos = player.eyePosition
        val rangeSq = max(SubmoduleCrystalPlacer.getMaxRange(), SubmoduleCrystalDestroyer.getMaxRange()).sq() + 1.0
        packet.runUpdates { blockPos, blockState ->
            if (blockState.isAir && tickIfInRange(blockPos, eyePos, rangeSq)) {
                return@runUpdates
            }
        }
    }

    private fun tickIfInRange(blockPos: BlockPos, eyePos: Vec3, rangeSq: Double): Boolean {
        val distance = eyePos.distanceToSqr(
            blockPos.x.toDouble(),
            blockPos.y.toDouble(),
            blockPos.z.toDouble()
        )

        if (distance < rangeSq) {
            runPlace { SubmoduleCrystalPlacer.tick() }
            return true
        }

        return false
    }

}
