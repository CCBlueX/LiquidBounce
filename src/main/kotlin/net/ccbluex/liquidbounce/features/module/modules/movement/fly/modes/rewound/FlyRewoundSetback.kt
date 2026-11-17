/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.rewound

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.stopXZVelocity
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3

/**
 * Jumps and freezes about a block up until the server sets back, then flies freely for a while.
 *
 * @anticheat NoCheatPlus
 * @testedOn rewound.cc
 */
internal object FlyRewoundSetback : Mode("RewoundSetback") {

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    private val speed by float("Speed", 1f, 0.1f..5f)
    private val duration by int("Duration", 3000, 500..10000, "ms")
    private val timer by float("Timer", 2f, 1f..5f)

    private enum class Stage { JUMP, FREEZE, FLY }

    private var stage = Stage.JUMP

    override fun enable() {
        stage = Stage.JUMP
        super.enable()
    }

    override fun disable() {
        player.stopXZVelocity()
        super.disable()
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent> { event ->
        if (stage == Stage.JUMP && player.onGround()) {
            event.jump = true
        }
    }

    @Suppress("unused")
    private val stageHandler = tickHandler {
        // The fourth air tick starts at 0.42 + 0.33 + 0.25, about one block up.
        tickUntil { player.airTicks >= 3 }
        stage = Stage.FREEZE

        tickUntil { stage == Stage.FLY }
        val flight = Chronometer(System.currentTimeMillis())
        tickUntil {
            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
            flight.hasElapsed(duration.toLong())
        }
        ModuleFly.enabled = false
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (stage != Stage.FREEZE) {
            return@handler
        }

        when (event.packet) {
            is ClientboundPlayerPositionPacket -> stage = Stage.FLY
            is ServerboundMovePlayerPacket -> event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (event.type != MoverType.SELF) {
            return@handler
        }

        val motion = when (stage) {
            Stage.JUMP -> return@handler
            Stage.FREEZE -> Vec3.ZERO
            Stage.FLY -> {
                val keys = player.input.keyPresses
                val y = when {
                    keys.jump && !keys.shift -> speed.toDouble()
                    keys.shift && !keys.jump -> -speed.toDouble()
                    else -> 0.0
                }
                Vec3(0.0, y, 0.0).withStrafe(speed = speed.toDouble())
            }
        }

        event.movement = motion
        player.deltaMovement = motion
    }
}
