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
package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.modes
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.entity.onGroundTicks
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.StairBlock
import kotlin.math.floor

/**
 * Ground-spoof criticals for Rewound (2020 Hypixel / NoCheatPlus fork), ported from LiquidBounce Legacy
 * and tested on rewound.cc.
 *
 * @anticheat NoCheatPlus (Rewound fork, based on April 2017)
 * @testedOn rewound.cc
 */
object CriticalsRewound20261112 : Mode("Rewound-2026-11-12", listOf("Rewound")) {

    override val parent: ModeValueGroup<Mode>
        get() = modes

    private val autoJump by boolean("AutoJump", false)
    private val packetOffset by boolean("PacketOffset", true)

    override fun enable() {
        if (autoJump && inGame && player.onGround()) {
            player.jumpFromGround()
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet !is ServerboundMovePlayerPacket || ModuleFly.running) {
            return@handler
        }

        if (!packetOffset) {
            packet.onGround = false
            return@handler
        }

        if (player.onGroundTicks < 3) {
            return@handler
        }

        val belowPos = BlockPos(floor(player.x).toInt(), floor(player.y - 1.0).toInt(), floor(player.z).toInt())
        val belowState = world.getBlockState(belowPos)
        if (belowState.isAir || belowState.block is SlabBlock || belowState.block is StairBlock) {
            return@handler
        }

        val offset = if (player.tickCount % 2 == 0) 1.0E-5 else 1.0E-6
        if (!packet.hasPos) {
            packet.x = player.x
            packet.y = player.y
            packet.z = player.z
            packet.hasPos = true
        }
        packet.y += offset
        packet.onGround = false
    }

}
