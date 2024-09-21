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
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.entity.upwards
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * abuses an exemption on some simulation anticheats allowing you to fly for a bit
 * after dismounting a vehicle (e.g. boat)
 * works on grim and on intave with the correct config
 */

internal object BoatLongJump : Choice("Boat") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private val horizontalSpeed by float("HorizontalSpeed", 1.3f, 1f..5f)
    private val verticalLaunch by float("VerticalLaunch", 0.42f, 0.1f..1.5f)
    private val ticksBeforeBoost by int("TicksBeforeBoost", 1, 0..20)
    private val flagsToBoostFrom by int("FlagsToBoostFrom", 3, 1..10)
    private var flags = 0

    override fun enable() {
        ModuleLongJump.jumped = false
        ModuleLongJump.boosted = false
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (event.packet) {
            is PlayerPositionLookS2CPacket -> {
                if (player.moving) {
                    flags++
                }
            }
        }
    }

    val repeatable = repeatable {
        if (flags >= flagsToBoostFrom) {
            waitTicks(ticksBeforeBoost)
            player.upwards(verticalLaunch)
            player.strafe(speed = horizontalSpeed.toDouble())
            flags = 0
            ModuleLongJump.boosted = true
            ModuleLongJump.jumped = true
        }
    }
}
