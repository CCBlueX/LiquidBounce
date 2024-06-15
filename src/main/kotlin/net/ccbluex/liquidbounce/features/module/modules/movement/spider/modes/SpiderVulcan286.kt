/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.cos
import kotlin.math.sin

internal object SpiderVulcan286 : Choice("Vulcan") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    /*
    * Vulcan mode for the Spider module.
    * Made for Vulcan286.
    * Tested on Eu.loyisa.cn
    * Sneaking seems to reduce flags a bit, but the way i do it is weird in the code, as its need holding sneak.
    * TODO: Detection for how many blocks you've gone up. Anything over 40ish seems to flag for Invalid (C)
    *  Proper implementation if there's something above you needs to be added.
    */

    private var glitch = true
    private var stop = false

    val repeatable = repeatable {

        val isOnLadder = player.blockPos.let { pos ->
            val block = world.getBlockState(pos).block
            block == Blocks.LADDER || block == Blocks.VINE
        }

        if (player.horizontalCollision && !isOnLadder) {
            stop = true
            waitTicks(2)
            player.velocity.y = 9.6599696
            waitTicks(2)
            player.velocity.y = 0.0001

        }
        if (player.horizontalCollision && !isOnLadder) {
            player.velocity.x = 0.0
            player.velocity.z = 0.0
            player.input.sneaking = true


        }

        if (stop && !player.horizontalCollision) {
            player.velocity.y = 0.0
            stop = false

        }
        // if (collidesVertical()) {
            //player.velocity.y = 0.0
        //}

    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        // Taken from LB Legacy's wall climb, seems to do something?

        if (packet is PlayerMoveC2SPacket && player.horizontalCollision && glitch) {
            val yaw = player.directionYaw

            packet.x -= sin(yaw) * 0.000000001
            packet.z += cos(yaw) * 0.000000001
            glitch = false
            packet.onGround = true

        }
    }
    //private fun collidesVertical() =
       // world.getBlockCollisions(player, player.boundingBox.offset(0.0, 0.5, 0.0)).any { shape ->
         //   shape != VoxelShapes.empty()
        //}
}
