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
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.isGroundWithin
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.movement.ncpBaseMoveSpeed
import net.ccbluex.liquidbounce.utils.movement.stopXZVelocity
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

/**
 * @anticheat NoCheatPlus
 * @testedOn rewound.cc
 */
internal object FlyRewound : Mode("Rewound") {

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    private val speed by float("Speed", 4.2f, 3.5f..5.5f)
    private val decay by float("Decay", 159f, 159f..600f)
    private val boostTimer by float("BoostTimer", 2f, 1f..3.5f)
    private val boostTicks by int("BoostTicks", 15, 1..40, "ticks")
    private val timer by boolean("Timer", false)

    private var moveSpeed = 0.0
    private var motionY: Double? = null
    private var boostTick = Int.MAX_VALUE

    private val currentSpeed
        get() = if (player.horizontalCollision || !player.moving) ncpBaseMoveSpeed() else moveSpeed

    override fun enable() {
        moveSpeed = 0.0
        motionY = null
        boostTick = Int.MAX_VALUE
        super.enable()
    }

    override fun disable() {
        player.stopXZVelocity()
        super.disable()
    }

    @Suppress("unused")
    private val flightHandler = tickHandler {
        if (player.onGround()) {
            selfDamage()
            waitTicks(1)
            moveSpeed = ncpBaseMoveSpeed(0.3)
            waitTicks(1)
            moveSpeed = currentSpeed * 2.149
            motionY = 0.3999
            waitTicks(1)
        }

        moveSpeed = ncpBaseMoveSpeed() * speed
        boostTick = 0
        while (true) {
            requestTimer()
            waitTicks(1)

            moveSpeed = currentSpeed - currentSpeed / decay
            motionY = 0.0
            if (!player.isGroundWithin(1e-4)) {
                player.setPos(player.x, player.y - 1e-14, player.z)
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ClientboundSetEntityMotionPacket if packet.id == player.id -> event.cancelEvent()
            is ClientboundPlayerPositionPacket -> {
                moveSpeed = ncpBaseMoveSpeed()
                boostTick = Int.MAX_VALUE
            }
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (event.type != MoverType.SELF) {
            return@handler
        }

        val y = motionY?.also { player.deltaMovement = player.deltaMovement.copy(y = it) } ?: event.movement.y
        motionY = null
        event.movement = Vec3(0.0, y, 0.0).withStrafe(speed = moveSpeed.coerceAtLeast(ncpBaseMoveSpeed()))
    }

    private fun requestTimer() {
        val normal = if (timer) 1.0766f else 1f
        val speed = if (boostTick < boostTicks && boostTimer > normal) {
            boostTimer - (boostTimer - normal) * boostTick / boostTicks
        } else {
            normal
        }
        if (boostTick < Int.MAX_VALUE) {
            boostTick++
        }
        Timer.requestTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
    }

    private fun selfDamage() {
        val offset = 0.425
        val iterations = ceil(3.0 / offset).toInt()
        val pos = player.position()
        val collision = player.horizontalCollision

        for (i in 0..iterations) {
            sendPacketSilently(ServerboundMovePlayerPacket.Pos(pos.x, pos.y + offset, pos.z, false, collision))
            sendPacketSilently(ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, i == iterations, collision))
        }
    }
}
