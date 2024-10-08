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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.modes
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * Strafe velocity
 */
internal object VelocityStrafe : Choice("Strafe") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    private val delay by int("Delay", 2, 0..10, "ticks")
    private val strength by float("Strength", 1f, 0.1f..2f)
    private val untilGround by boolean("UntilGround", false)

    private var applyStrafe = false

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        // Check if this is a regular velocity update
        if ((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) || packet is ExplosionS2CPacket) {
            // A few anti-cheats can be easily tricked by applying the velocity a few ticks after being damaged
            waitTicks(delay)

            // Apply strafe
            player.strafe(speed = player.sqrtSpeed * strength)

            if (untilGround) {
                applyStrafe = true
            }
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (player.isOnGround) {
            applyStrafe = false
        } else if (applyStrafe) {
            event.movement.strafe(player.directionYaw, player.sqrtSpeed * strength)
        }
    }

}
