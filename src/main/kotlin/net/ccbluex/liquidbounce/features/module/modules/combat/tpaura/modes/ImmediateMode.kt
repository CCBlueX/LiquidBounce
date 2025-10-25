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
 */

package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.modes

import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.clicker
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.desyncPlayerPosition
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.stuckChronometer
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.targetSelector
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.TpAuraChoice
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.floor

object ImmediateMode : TpAuraChoice("Immediate") {

    val repeatable = tickHandler {
        if (!clicker.isClickTick) {
            return@tickHandler
        }

        val playerPosition = player.pos
        val enemyPosition = targetSelector.targets().minByOrNull { it.squaredBoxedDistanceTo(playerPosition) }?.pos
            ?: return@tickHandler

        travel(enemyPosition)
        waitTicks(20)
        travel(playerPosition)
        desyncPlayerPosition = null
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            desyncPlayerPosition?.let { playerPosition ->
                drawLineStrip(
                    Color4b.WHITE.toARGB(),
                    relativeToCamera(player.pos.add(0.0, 1.0, 0.0)).toVec3(),
                    relativeToCamera(playerPosition.add(0.0, 1.0, 0.0)).toVec3()
                )
            }
        }
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            val position = desyncPlayerPosition ?: return@handler

            // Set the packet position to the player position
            packet.x = position.x
            packet.y = position.y
            packet.z = position.z
            packet.changePosition = true
        } else if (packet is PlayerPositionLookS2CPacket) {
            chat(markAsError("Server setback detected - teleport failed!"))
            stuckChronometer.reset()
            desyncPlayerPosition = null
        }
    }

    private fun travel(position: Vec3d) {
        val x = position.x
        val y = position.y
        val z = position.z

        val deltaX = x - player.x
        val deltaY = y - player.y
        val deltaZ = z - player.z

        val times = (floor((abs(deltaX) + abs(deltaY) + abs(deltaZ)) / 10) - 1).toInt()
        val packetToSend = MovePacketType.FULL
        repeat(times) {
            network.sendPacket(packetToSend.generatePacket().apply {
                this.x = player.x
                this.y = player.y
                this.z = player.z
                this.yaw = player.yaw
                this.pitch = player.pitch
                this.onGround = player.isOnGround
            })
        }

        network.sendPacket(packetToSend.generatePacket().apply {
            this.x = x
            this.y = y
            this.z = z
            this.yaw = player.yaw
            this.pitch = player.pitch
            this.onGround = player.isOnGround
        })

        desyncPlayerPosition = position
    }

}
