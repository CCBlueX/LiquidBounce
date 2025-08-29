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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
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
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes

/**
 * @anticheat Vulcan
 * @anticheat Version 2.8.6
 * @testedOn eu.loyisa.cn, anticheat-test.com
 * @note ONLY WORKS ON 1.8 SERVERS
 * @author Nullable
 */
internal object FlyVulcan286MC18 : Choice("Vulcan286-18") {


    // 2.5 is the maximum timer tested.
    private val timer by float("Timer", 2.5f, 1f..2.5f)
    private val autoDisable by boolean("AutoDisable", false)

    override val parent: ChoiceConfigurable<*>
        get() = modes

    var flags = 0
    private var flagPos: Vec3d? = null

    override fun enable() {
        flags = 0
        flagPos = null
        chat(regular(message("vulcanGhostOldMessage")))
    }

    val tickHandler = handler<PlayerTickEvent> {
        if (flags > 1) {
            Timer.requestTimerSpeed(timer, Priority.NORMAL, ModuleFly, 1)
            /**
             * 1.8 vulcan allows timer while desynced, 1.9 doesn't.
             */
        }
    }


    /**
     * When you flag (any PlayerPositionLookS2CPacket packet works),
     * vanilla desyncs you. While desynced, you can timer
     * and flagging ghost block check sets you back to
     * the new position while in desync...
     *
     * 1.8 servers spam the flag, so when you flag for
     * ghostblock fix, it switches the spammed flag to
     * the new position.
     */

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        if (packet is PlayerPositionLookS2CPacket) {
            flags++
            if (autoDisable) {
                val pos = packet.change.position
                if (flags == 2) {
                    flagPos = pos
                } else if (flags > 2 && flagPos != pos) {
                    ModuleFly.enabled = false
                    return@handler
                    /**
                     * If we didn't return, we would have to wait 1 tick
                     * for a new PlayerPositionLook
                     */
                }
            }
            it.cancelEvent()
        }
    }

    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.pos == player.blockPos.down() && !player.isSneaking) {
            event.shape = VoxelShapes.fullCube()
        } else {
            event.shape = VoxelShapes.empty()
        }
    }

}

