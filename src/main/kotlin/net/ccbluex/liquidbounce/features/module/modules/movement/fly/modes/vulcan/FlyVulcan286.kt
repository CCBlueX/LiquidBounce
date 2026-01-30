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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.message
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.shapes.Shapes

/**
 * @anticheat Vulcan
 * @anticheat Version 2.8.6
 * @testedOn localhost
 * @note ONLY WORKS ON 1.13+ SERVERS
 */
internal object FlyVulcan286 : Mode("Vulcan286-113") {

    override val parent: ModeValueGroup<*>
        get() = modes

    var packet: ClientboundPlayerPositionPacket? = null
    var flags = 0
    var wait = false

    override fun enable() {
        packet = null
        wait = false
        flags = 0
        chat(regular(message("vulcanGhostNewMessage")))

        // Send Packet to desync
        network.send(
            ServerboundMovePlayerPacket.PosRot(
                player.x, player.y - 0.1, player.z,
                player.yRot, player.xRot, player.onGround(), player.horizontalCollision
            )
        )
    }

    override fun disable() {
        packet?.let {
            handlePacket(it)
        }
    }

    val tickHandler = handler<PlayerTickEvent> {
        if (mc.options.keyUse.isDown) {
            packet?.let {
                handlePacket(it)
            }
            packet = null
            wait = true
        }
    }

    val moveHandler = handler<PlayerMoveEvent> { event ->
        if (!world.getBlockState(player.blockPosition().below()).isAir && wait) {
            event.movement = event.movement.copy(x = 0.0, z = 0.0)
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ClientboundPlayerPositionPacket) {
            flags++
            if (flags == 1) {
                packet = event.packet
                event.cancelEvent()
            } else {
                ModuleFly.enabled = false
            }
        }
    }

    @Suppress("unused")
    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.pos == player.blockPosition().below() && !player.isShiftKeyDown) {
            event.shape = Shapes.block()
        } else if (!wait) {
            event.shape = Shapes.empty()
        }
    }

}
