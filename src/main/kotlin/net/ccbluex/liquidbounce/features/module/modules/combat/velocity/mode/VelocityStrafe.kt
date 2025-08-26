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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * Strafe velocity
 */
internal object VelocityStrafe : VelocityMode("Strafe") {

    private val delay by int("Delay", 2, 0..10, "ticks")
    private val strength by float("Strength", 1f, 0.1f..2f)

    object OnlyFacing: ToggleableConfigurable(this, "OnlyFacing", false) {
        val range by float("Range", 3.5f, 0.1f..6f)
    }

    init {
        tree(OnlyFacing)
    }

    private val untilGround by boolean("UntilGround", false)

    private var applyStrafe = false
    private var shouldStrafe = false

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (!OnlyFacing.enabled) return@handler
        val target = world.findEnemy(0f..OnlyFacing.range) ?: return@handler

        val isFacingEnemy = facingEnemy(
            target,
            OnlyFacing.range.toDouble(),
            RotationManager.currentRotation ?: player.rotation
        )

        shouldStrafe = isFacingEnemy
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        // Check if this is a regular velocity update
        if ((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) || packet is ExplosionS2CPacket) {
            if (OnlyFacing.enabled && !shouldStrafe) {
                return@sequenceHandler
            }

            // A few anti-cheats can be easily tricked by applying the velocity a few ticks after being damaged
            waitTicks(delay)

            // Apply strafe
            player.velocity = player.velocity.withStrafe(speed = player.sqrtSpeed * strength)

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
            event.movement = event.movement.withStrafe(speed = player.sqrtSpeed * strength)
        }
    }

}
