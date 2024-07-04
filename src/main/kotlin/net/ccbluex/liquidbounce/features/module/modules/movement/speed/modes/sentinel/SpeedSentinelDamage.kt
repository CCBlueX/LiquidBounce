/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.sentinel

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePingSpoof
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.movement.zeroXZ
import net.minecraft.entity.MovementType
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import kotlin.math.floor

/**
 * @anticheat Sentinel
 * @anticheatVersion 30.06.2024
 * @testedOn cubecraft.net
 */
class SpeedSentinelDamage(override val parent: ChoiceConfigurable<*>) : Choice("SentinelDamage") {

    private val speed by float("Speed", 0.3f, 0.1f..5f)
    private val reboostTicks by int("ReboostTicks", 30, 10..50)

    val movementInputEvent = handler<MovementInputEvent> {
        if (player.moving && hasBeenHurt) {
            it.jumping = true
        }
    }

    private var hasBeenHurt = false
    private var adjusted = false
    private var damageDelay = 0
    private var enabledTime = 0L

    override fun enable() {
        if (!ModulePingSpoof.enabled) {
            ModulePingSpoof.enabled = true
        }
        hasBeenHurt = false
        damageDelay = 0
        enabledTime = System.currentTimeMillis()

        super.enable()
    }

    val repeatable = repeatable {
        boost()
        waitTicks(reboostTicks)
    }

    override fun disable() {
        player.zeroXZ()
    }

    val moveHandler = handler<PlayerMoveEvent> { event ->
        if (ModuleFly.enabled) {
            ModuleSpeed.enabled = false
            return@handler
        }

        if (player.hurtTime > 0  && !hasBeenHurt) {
            hasBeenHurt = true
            damageDelay = floor(enabledTime/50.0).toInt()
            adjusted = true
        }

        if (!hasBeenHurt && !adjusted) {
            return@handler
        }

        if (event.type == MovementType.SELF && player.moving) {
            val movement = event.movement
            movement.strafe(player.directionYaw, strength = 1.0, speed = speed.toDouble())
        }
    }

    private fun boost() {
        hasBeenHurt = false
        enabledTime = System.currentTimeMillis()
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
        network.sendPacket(
            PlayerMoveC2SPacket.PositionAndOnGround(
                player.x, player.y + 3.25, player.z,
            false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, true))
    }

}
