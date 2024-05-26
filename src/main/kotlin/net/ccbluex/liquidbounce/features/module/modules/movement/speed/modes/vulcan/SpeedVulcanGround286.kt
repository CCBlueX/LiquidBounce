/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.entity.effect.StatusEffects
import net.ccbluex.liquidbounce.event.repeatable
import net.minecraft.util.shape.VoxelShapes

/**
 * @anticheat Vulcan
 * @anticheatVersion V2.8.6
 * @testedOn anticheat-test.com, eu.loyisa.cn
 * @note flags on specific blocks such as fences
 */
object SpeedVulcanGround286 : SpeedBHopBase("VulcanGround286") {
    val repeatable = repeatable {
        if (player.moving && collidesBottomVertical()) {
            if (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0 == 0) {
                player.strafe(speed = if (player.input.movementSideways != 0f) 0.41 else 0.42)
            } else {
                player.strafe(speed = if (player.input.movementSideways != 0f) 0.59 else 0.59)
            }
            player.velocity.y = 0.005
        }
    }
    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerMoveC2SPacket && collidesBottomVertical()) {
            event.packet.y += 0.005
        }
    }
    private fun collidesBottomVertical() =
        world.getBlockCollisions(player, player.boundingBox.offset(0.0, -0.005, 0.0)).any { shape ->
            shape != VoxelShapes.empty()
        }
    val jumpEvent = handler<PlayerJumpEvent> { event ->
        event.cancelEvent()
    }
}
