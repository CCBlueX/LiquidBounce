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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.verus

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.stopXZVelocity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * @anticheat Verus
 * @anticheatVersion b3896
 * @testedOn eu.loyisa.cn
 * @note it gives you ~2 flags for damage
 */
internal object FlyVerusB3896Damage : Choice("VerusB3896Damage") {

    override val parent: ChoiceConfigurable<*>
        get() = modes

    private var flyTicks = 0
    private var shouldStop = false
    private var gotDamage = false

    override fun enable() {
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false,
            player.horizontalCollision))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y + 3.25, player.z, false,
            player.horizontalCollision))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false,
            player.horizontalCollision))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, true,
            player.horizontalCollision))
    }

    @Suppress("unused")
    val failRepeatable = tickHandler {
        if (!gotDamage) {
            waitTicks(20)
            if (!gotDamage) {
                chat("Failed to self-damage")
                shouldStop = true
            }
        }
    }
    val repeatable = tickHandler {
        if (player.hurtTime > 0) {
            gotDamage = true
        }

        if (!gotDamage) {
            return@tickHandler
        }

        if (++flyTicks > 20 || shouldStop) {
            ModuleFly.enabled = false
            return@tickHandler
        }

        player.velocity = player.velocity.withStrafe(speed = 9.95)
        player.velocity.y = 0.0
        Timer.requestTimerSpeed(0.1f, Priority.IMPORTANT_FOR_USAGE_2, ModuleFly)
    }

    override fun disable() {
        flyTicks = 0
        player.stopXZVelocity()
    }
}
