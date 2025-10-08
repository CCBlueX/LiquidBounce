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

package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Vec3d

/**
 * Does not work in server version 1.8 and below.
 *
 * @author jiuxian_baka
 */
internal object NoFallSpoofLanding : NoFallMode("SpoofLanding") {

    private val modification by vec3d("Modification", Vec3d(1337.0, 0.0, 1337.0))

    @Volatile
    private var prevFallDistance = 0.0

    @Volatile
    private var prevOnGround = false

    @Volatile
    private var flag = false

    override fun disable() {
        prevFallDistance = 0.0
        prevOnGround = false
        flag = false
        super.disable()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        if (isOlderThanOrEqual1_8) {
            chat(markAsError(translation("liquidbounce.module.noFall.messages.spoofLanding")))
            ModuleNoFall.enabled = false
            return@handler
        }
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            if (packet.onGround && !prevOnGround && prevFallDistance >= playerSafeFallDistance) {
                flag = true
                val (dx, dy, dz) = modification
                packet.x += dx
                packet.y += dy
                packet.z += dz
                packet.onGround = false
                player.onLanding()
            }

            prevOnGround = packet.onGround
            prevFallDistance = player.fallDistance.toDouble()
        }
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = MODEL_STATE) { event ->
        if (flag && player.isOnGround) {
            event.sprint = false
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent> { event ->
        debugParameter("shouldJump") { flag }
        if (flag && player.isOnGround) {
            event.jump = true
            flag = false
        }
    }
}
