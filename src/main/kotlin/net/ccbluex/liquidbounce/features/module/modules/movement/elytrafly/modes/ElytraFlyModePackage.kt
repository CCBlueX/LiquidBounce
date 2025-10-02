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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

internal object ElytraFlyModePackage : ElytraFlyMode("Package") {

    private val speed by float("Speed", 1.5f, 0.5f..3.0f)
    private val acceleration by float("Acceleration", 0.05f, 0.01f..0.2f)
    private val deceleration by float("Deceleration", 0.03f, 0.01f..0.15f)
    private val verticalSpeed by float("VerticalSpeed", 0.5f, 0.1f..2.0f)
    private val smoothing by boolean("Smoothing", true)
    private val antiKick by boolean("AntiKick", true)
    private val glideOnStop by boolean("GlideOnStop", true)
    private val bypassMode by enumChoice("BypassMode", BypassMode.VANILLA)

    private var currentSpeed = 0.0
    private var targetSpeed = 0.0
    private var motionY = 0.0
    private var tickCounter = 0
    private var lastPosition = Vec3d.ZERO

    override fun enable() {
        resetState()
    }

    override fun disable() {
        resetState()
    }

    private fun resetState() {
        currentSpeed = 0.0
        targetSpeed = 0.0
        motionY = 0.0
        tickCounter = 0
        lastPosition = player.pos
    }

    override fun onTick() {
        if (!player.isGliding) {
            resetState()
            return
        }

        tickCounter++
        updateTargetSpeed()
        updateCurrentSpeed()
        
        if (antiKick && tickCounter % 20 == 0) {
            applyAntiKickMeasures()
        }
    }

    private fun updateTargetSpeed() {
        targetSpeed = when {
            player.moving && mc.options.jumpKey.isPressed -> speed.toDouble() * 1.2
            player.moving -> speed.toDouble()
            glideOnStop -> speed.toDouble() * 0.3
            else -> 0.0
        }
    }

    private fun updateCurrentSpeed() {
        currentSpeed = when {
            currentSpeed < targetSpeed -> {
                (currentSpeed + acceleration).coerceAtMost(targetSpeed)
            }
            currentSpeed > targetSpeed -> {
                (currentSpeed - deceleration).coerceAtLeast(targetSpeed)
            }
            else -> currentSpeed
        }
    }

    private fun applyAntiKickMeasures() {
        when (bypassMode) {
            BypassMode.VANILLA -> motionY -= 0.001
            BypassMode.UPDATED -> {
                if (tickCounter % 40 == 0) {
                    motionY -= 0.005
                }
            }
            BypassMode.STRICT -> {
                if (!mc.options.jumpKey.isPressed && !mc.options.sneakKey.isPressed) {
                    motionY -= 0.008
                }
            }
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (ModuleElytraFly.shouldNotOperate() || !player.isGliding) {
            return@handler
        }

        handleVerticalMovement()
        
        if (player.moving || (glideOnStop && currentSpeed > 0)) {
            val movementSpeed = if (smoothing) {
                currentSpeed
            } else {
                speed.toDouble()
            }
            
            event.movement = event.movement.withStrafe(speed = movementSpeed)
        }

        event.movement = Vec3d(event.movement.x, motionY, event.movement.z)
        lastPosition = player.pos.add(event.movement)
    }

    private fun handleVerticalMovement() {
        motionY = when {
            mc.options.jumpKey.isPressed && !mc.options.sneakKey.isPressed -> {
                verticalSpeed.toDouble()
            }
            mc.options.sneakKey.isPressed && !mc.options.jumpKey.isPressed -> {
                -verticalSpeed.toDouble()
            }
            glideOnStop && !player.moving -> {
                -0.02
            }
            else -> {
                calculateNaturalMotion()
            }
        }
    }

    private fun calculateNaturalMotion(): Double {
        val pitchRadians = Math.toRadians(player.pitch.toDouble())
        return when (bypassMode) {
            BypassMode.VANILLA -> -0.01
            BypassMode.UPDATED -> sin(pitchRadians) * 0.02
            BypassMode.STRICT -> {
                val naturalGravity = -0.015
                val pitchInfluence = sin(pitchRadians) * 0.01
                naturalGravity + pitchInfluence
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ClientCommandC2SPacket) {
            if (packet.mode == ClientCommandC2SPacket.Mode.START_FALL_FLYING && !player.isGliding) {
                lastPosition = player.pos
            }
        }
    }

    private enum class BypassMode(override val choiceName: String) : net.ccbluex.liquidbounce.config.types.NamedChoice {
        VANILLA("Vanilla"),
        UPDATED("Updated"),
        STRICT("Strict")
    }
}
