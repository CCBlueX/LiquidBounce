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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.Entity
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import kotlin.math.min

/**
 * TickBase
 *
 * Calls tick function to speed up, when needed
 */
internal object TickBase : ToggleableConfigurable(ModuleKillAura, "Tickbase", false) {

    private val balanceRecoveryIncrement by float("BalanceRecoverIncrement", 1f, 0f..2f)
    private val balanceMaxValue by int("BalanceMaxValue", 20, 0..200)
    private val maxTicksAtATime by int("MaxTicksAtATime", 4, 1..20, "ticks")
    private val pauseOnFlag by boolean("PauseOfFlag", true)
    private val pauseAfterTick by int("PauseAfterTick", 0, 0..100, "ticks")
    private val forceGround by boolean("ForceGround", false)

    private var ticksToSkip = 0
    private var tickBalance = 0f
    private var reachedTheLimit = false

    private val tickBuffer = mutableListOf<TickData>()

    private val rangeToAttack: Double
        get() = ModuleKillAura.range.toDouble()

    private val target: Entity?
        get() = ModuleKillAura.targetTracker.lockedOnTarget

    val tickHandler = handler<PlayerTickEvent> {
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.enabled) {
            return@handler
        }

        if (ticksToSkip-- > 0) {
            it.cancelEvent()
        }
    }

    private var duringTickModification = false

    val postTickHandler = handler<PlayerPostTickEvent> {
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.enabled || duringTickModification) {
            return@handler
        }

        if (tickBuffer.isEmpty()) {
            return@handler
        }

        val nearbyEnemy = target ?: return@handler

        // Find the best tick that is able to hit the target and is not too far away from the player, as well as
        // able to crit the target
        val possibleTicks = tickBuffer
            .mapIndexed { index, tick -> index to tick }
            .filter { (_, tick) ->
                tick.position.distanceTo(nearbyEnemy.pos) <= rangeToAttack
            }
            .filter { (_, tick) ->
                !forceGround || tick.onGround
            }

        val criticalTick = possibleTicks
            .filter { (_, tick) ->
                tick.fallDistance > 0.0f
            }
            .minByOrNull { (index, _) ->
                index
            }
        val (bestTick, _) = criticalTick ?: possibleTicks.minByOrNull { (index, _) ->
            index
        } ?: return@handler

        if (bestTick == 0) {
            return@handler
        }

        if (!ModuleKillAura.clickScheduler.isClickOnNextTick(bestTick)) {
            return@handler
        }

        // Tick as much as we can
        duringTickModification = true
        repeat(bestTick) {
            player.tick()
            tickBalance -= 1
        }
        ticksToSkip = bestTick + pauseAfterTick
        duringTickModification = false
    }

    val inputHandler = handler<MovementInputEvent> { event ->
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.enabled) {
            return@handler
        }

        tickBuffer.clear()

        val input = SimulatedPlayer.SimulatedPlayerInput(event.directionalInput, player.input.jumping,
            player.isSprinting, player.isSneaking)
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)

        if (tickBalance <= 0) {
            reachedTheLimit = true
        }
        if (tickBalance > balanceMaxValue / 2) {
            reachedTheLimit = false
        }
        if (tickBalance <= balanceMaxValue) {
            tickBalance += balanceRecoveryIncrement
        }

        if (reachedTheLimit) {
            return@handler
        }

        repeat(min(tickBalance.toInt(), maxTicksAtATime)) {
            simulatedPlayer.tick()
            tickBuffer += TickData(
                simulatedPlayer.pos,
                simulatedPlayer.fallDistance,
                simulatedPlayer.velocity,
                simulatedPlayer.onGround
            )
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            withColor(Color4b.BLUE) {
                drawLineStrip(lines = tickBuffer.map { tick -> tick.position.toVec3() }.toTypedArray())
            }
        }
    }

    val packetHandler = handler<PacketEvent> {
        // Stops when you got flagged
        if (it.packet is PlayerPositionLookS2CPacket && pauseOnFlag) {
            tickBalance = 0f
        }
    }

    data class TickData(
        val position: Vec3d,
        val fallDistance: Float,
        val velocity: Vec3d,
        val onGround: Boolean
    )

}
