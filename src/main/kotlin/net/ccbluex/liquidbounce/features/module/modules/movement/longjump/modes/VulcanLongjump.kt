/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.movement.zeroXZ
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * @anticheat Vulcan
 * @anticheatVersion 2.8.9
 * @testedOn anticheat-test.com, eu.loyisa.cn
 */
internal object VulcanLongjump : Choice("Vulcan") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode
    var recievedLagback = false
    var started = false

    override fun enable() {
        recievedLagback = false
        started = false
        ModuleLongJump.jumped = false
        ModuleLongJump.boosted = false
    }

    val repeatable = repeatable {

        val positions = listOf(
            0.41999998688698,
            0.7531999805212,
            1.00133597911214,
            1.16610926093821,
            1.24918707874468,
            1.25220334025373,
            1.17675927506424,
            1.02442408821369,
            0.79673560066871,
            0.49520087700593,
            0.1212968405392,
            0
        )

        if (started && player.hurtTime == 10) {
            player.setPosition(player.pos.x, player.pos.y - 0.5, player.pos.z)
        }
        if (started && recievedLagback) {
            player.velocity.y = 1.0
            player.setPosition(player.pos.x, player.pos.y + 8, player.pos.z)
            player.strafe(strength = 1.0, speed = 4.2)
            recievedLagback = false
        }
        if (started && player.hurtTime == 5) {
            player.setPosition(player.pos.x, player.pos.y + 8, player.pos.z)
            player.strafe(strength = 1.0, speed = 0.3)
            started = false
            ModuleLongJump.jumped = true
            ModuleLongJump.boosted = true
        }
        if(player.age % 2 == 0) {
            player.velocity.y = -0.0971
        } else {
            player.velocity.y = -0.148
        }
        if (player.isOnGround && !recievedLagback && player.hurtTime == 0) {
            if(!(ModuleLongJump.autoDisable && ModuleLongJump.jumped)) {
                repeat(3) {
                    for (position in positions) {
                        network.sendPacket(
                            PlayerMoveC2SPacket.PositionAndOnGround(
                                player.pos.x,
                                player.pos.y + position.toDouble(),
                                player.pos.z,
                                false
                            )
                        )
                    }
                }
            }
            started = true
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is PlayerPositionLookS2CPacket) {
            recievedLagback = true
        }
    }

}
