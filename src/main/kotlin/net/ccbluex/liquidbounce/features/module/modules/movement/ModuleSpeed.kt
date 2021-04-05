/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Choice
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.sequenceHandler
import net.ccbluex.liquidbounce.utils.extensions.downwards
import net.ccbluex.liquidbounce.utils.extensions.moving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.upwards
import net.minecraft.entity.effect.StatusEffects
import kotlin.math.hypot

object ModuleSpeed : Module("Speed", Category.MOVEMENT) {

    private val modes = choices("Mode", "YPort") {
        SpeedYPort
        HypixelHop
    }

    private object SpeedYPort : Choice("YPort", modes) {

        val tickHandler = sequenceHandler<PlayerTickEvent> {
            if (player.isOnGround && player.moving) {
                player.strafe(speed = 0.4)
                player.upwards(0.42f)
                wait(1)
                player.downwards(-1f)
            }
        }
    }

    private object HypixelHop : Choice("HypixelHop", modes) {
        val playerNetworkMovementTick = handler<PlayerNetworkMovementTickEvent> { event ->

            if (event.state != EventState.PRE)
                return@handler

            if (player.moving) {
                if (player.isOnGround) {
                    player.updatePosition(player.x, player.y + 9.1314E-4, player.z)
                    player.jump()
                    player.strafe(speed = ((if (getSpeed() < 0.56f) getSpeed() * 1.045f else 0.56f) * (1f + 0.13f * getSpeedEffectAmplifier())).toDouble())
                } else if (player.velocity.y < 0.2)
                    player.velocity.y -= 0.02
                player.strafe(speed = (getSpeed() * 1.01889f).toDouble())
            } else {
                player.velocity.x = 0.0
                player.velocity.z = 0.0
            }
        }
    }

    fun getSpeedEffectAmplifier() =
        player.getStatusEffect(StatusEffects.SPEED)!!.amplifier.plus(1)

    fun getSpeed(): Float {
        val mX = player.velocity.x
        val mZ = player.velocity.z
        return hypot(mX, mZ).toFloat()
    }
}
