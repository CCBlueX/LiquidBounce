/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.enabled
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.zeroXZ
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

    var flyTicks = 0
    var shouldStop = false
    var gotDamage = false

    override fun enable() {
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y + 3.25, player.z, false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, true))
    }

    @Suppress("unused")
    val failRepeatable = repeatable {
        if (!gotDamage) {
            waitTicks(20)
            if (!gotDamage) {
                chat("Failed to self-damage")
                shouldStop = true
            }
        }
    }
    val repeatable = repeatable {
        if (player.hurtTime > 0) {
            gotDamage = true
        }

        if (!gotDamage) {
            return@repeatable
        }

        if (++flyTicks > 20 || shouldStop) {
            enabled = false
            return@repeatable
        }

        player.strafe(speed = 9.95)
        player.velocity.y = 0.0
        Timer.requestTimerSpeed(0.1f, Priority.IMPORTANT_FOR_USAGE_2, ModuleFly)
    }

    override fun disable() {
        flyTicks = 0
        player.zeroXZ()
    }
}
