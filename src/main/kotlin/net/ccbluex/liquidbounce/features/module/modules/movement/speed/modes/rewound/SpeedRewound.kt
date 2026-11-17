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

package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.rewound

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerStepSuccessEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.effectLevel
import net.ccbluex.liquidbounce.utils.entity.isGroundWithin
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.movement.ncpBaseMoveSpeed
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * @anticheat NoCheatPlus
 * @testedOn rewound.cc
 */
class SpeedRewound(override val parent: ModeValueGroup<*>) : Mode("Rewound") {

    private val timer by boolean("Timer", true)

    private enum class Phase {
        /** Base speed for [phaseTicks] moves after a setback or wall hit. */
        RECOVER,
        BOOST,
        AIR,
    }

    private var phase = Phase.AIR
    private var phaseTicks = 2
    private var collided = false
    private var stairTicks = 0
    private var hopLoad = 0.0
    private var slowHop = false
    private var slowAfterSetback = false
    private val lastSetback = Chronometer()

    private val isTowering
        get() = ModuleScaffold.running && mc.options.keyJump.isDown

    override fun enable() {
        phase = Phase.AIR
        phaseTicks = 2
        collided = player.horizontalCollision
        hopLoad = 0.0
        slowHop = false
        super.enable()
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (player.moving && !isTowering) {
            event.jump = false
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ClientboundPlayerPositionPacket) {
            recover(4)
            lastSetback.reset()
        }
    }

    @Suppress("unused")
    private val stepHandler = handler<PlayerStepSuccessEvent> { event ->
        val height = event.adjustedVec.y
        if (height > 0.7) {
            hopLoad = 0.0
        }
        if (abs(height - 0.5) < 1e-4) {
            stairTicks = 3
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (event.type != MoverType.SELF || isTowering) {
            return@handler
        }

        collided = collided || player.horizontalCollision
        if (collided) {
            recover(1)
        } else if (timer) {
            Timer.requestTimerSpeed(1.2f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        }

        stairTicks = (stairTicks - 1).coerceAtLeast(0)
        hopLoad = (hopLoad - if (hopLoad > 1.0) 0.12 else 0.11).coerceAtLeast(0.0)

        if (!player.moving) {
            return@handler
        }

        var y = event.movement.y
        if (!player.isInLiquid && player.isGroundWithin(0.01)) {
            collided = player.horizontalCollision
            if (phase != Phase.RECOVER || collided) {
                phase = Phase.BOOST
                if (stairTicks == 0) {
                    y = 0.407 + player.effectLevel(MobEffects.JUMP_BOOST) * 0.1
                    player.deltaMovement = player.deltaMovement.copy(y = y)
                }

                // Hops less than about 9 ticks apart pile up, every second one of those is 5% slower.
                hopLoad++
                slowHop = hopLoad > 1.0 && !slowHop
                hopLoad = hopLoad.coerceAtMost(1.12)
            }
        }

        if (phase == Phase.BOOST) {
            slowAfterSetback = !lastSetback.hasElapsed(500)
        }

        event.movement = Vec3(0.0, y, 0.0).withStrafe(speed = moveSpeed())
        advance()
    }

    private fun recover(ticks: Int) {
        phase = Phase.RECOVER
        phaseTicks = ticks
    }

    private fun advance() {
        when (phase) {
            Phase.RECOVER -> if (--phaseTicks == 0) phase = Phase.BOOST
            Phase.BOOST -> {
                phase = Phase.AIR
                phaseTicks = 1
            }
            Phase.AIR -> phaseTicks++
        }
    }

    private fun moveSpeed(): Double {
        if (player.isInLiquid) {
            return 0.12
        }

        var speed = when {
            phase == Phase.RECOVER -> ncpBaseMoveSpeed()
            stairTicks > 0 -> (hopSpeed() + 0.0331) * 0.91 * (0.7 - player.effectLevel(MobEffects.SPEED) * 0.1)
            else -> (hopSpeed() + 0.0331) * 0.91
        }
        if (slowHop) {
            speed *= 0.95
        }
        return speed
    }

    private fun hopSpeed(): Double {
        if (slowAfterSetback) {
            return if (phase == Phase.BOOST) 0.0 else 0.2
        }

        val level = player.effectLevel(MobEffects.SPEED)
        val floor = ncpBaseMoveSpeed() + 0.028 * level
        if (!lastSetback.hasElapsed(500) || collided) {
            return floor
        }

        val first = 0.4145 + level / 12.5
        val speed = when {
            phase == Phase.BOOST -> 0.64 + (level + 0.028 * level) * 0.134
            phaseTicks == 1 -> first
            else -> first - phaseTicks / 250.0
        }
        return speed.coerceAtLeast(floor)
    }
}
