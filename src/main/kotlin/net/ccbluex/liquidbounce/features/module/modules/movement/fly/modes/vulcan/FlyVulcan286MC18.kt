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
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.message
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes

/**
 * @anticheat Vulcan
 * @anticheat Version 2.8.6
 * @testedOn eu.loyisa.cn, anticheat-test.com
 * @note ONLY WORKS ON 1.8 SERVERS
 * @author Nullable
 */
internal object FlyVulcan286MC18 : Mode("Vulcan286-18") {


    // 2.5 is the maximum timer tested.
    private val timer by float("Timer", 2.5f, 1f..2.5f)
    private val autoDisable by boolean("AutoDisable", false)

    override val parent: ModeValueGroup<*>
        get() = modes

    var flags = 0
    private var flagPos: Vec3? = null

    override fun enable() {
        flags = 0
        flagPos = null
        chat(regular(message("vulcanGhostOldMessage")))
    }

    val tickHandler = handler<PlayerTickEvent> {
        if (flags > 1) {
            // 1.8 vulcan allows timer while desynced, 1.9 doesn't.
            Timer.requestTimerSpeed(timer, Priority.NORMAL, ModuleFly, 1)
        }
    }


    /**
     * When you flag (any ClientBoundPlayerPositionPacket works),
     * vanilla server stops you. If for some reason your client doesn't
     * receive the packet, the players serverside position will
     * not change when moving. This will provide a "desynced state"
     *
     * While desynced, you can use timer freely, and
     * flagging the ghost block check sets you back to
     * the position you are in while desynced.
     *
     * 1.8 servers spam ClientBoundPlayerPositionPackets
     * so when you flag for ghost block check,
     * it switches the spammed packet to
     * the new position.
     *
     * NOTE: ghost block check works by checking if you
     * are walking on air after walking off of a ledge.
     * This check can be triggered while desynced...
     */

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        if (packet is ClientboundPlayerPositionPacket) {
            flags++
            if (autoDisable) {
                val pos = packet.change.position
                if (flags == 2) {
                    flagPos = pos
                } else if (flags > 2 && flagPos != pos) {
                    ModuleFly.enabled = false
                    // Return here so we accept this packet
                    return@handler
                }
            }
            it.cancelEvent()
        }
    }

    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.pos == player.blockPosition().below() && !player.isShiftKeyDown) {
            event.shape = Shapes.block()
        } else {
            event.shape = Shapes.empty()
        }
    }

}

