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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.hypixel

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * @anticheat Watchdog (NCP)
 * @anticheatVersion 21.01.25
 * @testedOn hypixel.net
 * @author @liquidsquid1
 */
object FlyHypixelFlat : Choice("HypixelFlat") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    private val timer by float("Timer", 1.0f, 0.1f..1.0f)
    private val flySpeed by float("Speed", 1.66f, 0.8f..2.0f)

    private var flyTicks = 0
    private var isFlying = false

    override fun disable() {
        flyTicks = 0
        isFlying = false
        super.disable()
    }

    @Suppress("unused")
    private val speedHandler = tickHandler {
        waitUntil { isFlying }

        player.velocity = player.velocity.withStrafe(speed = 0.8)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = flySpeed.toDouble())

        waitUntil { player.isOnGround }
        ModuleFly.enabled = false
    }

    @Suppress("unused")
    private val velocityHandler = tickHandler {
        if (!isFlying) {
            return@tickHandler
        }

        flyTicks++
        if (flyTicks > 30) {
            return@tickHandler
        }

        Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
        player.velocity.y = 0.0314 + (Math.random() / 1000f)
        player.velocity = player.velocity.withStrafe(speed = player.sqrtSpeed)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ExplosionS2CPacket) {
            isFlying = true
        }
    }

}
