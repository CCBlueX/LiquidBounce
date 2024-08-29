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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
 * @anticheat Watchdog (NCP)
 * @anticheatVersion 28.8.2024
 * @testedOn hypixel.net
 */
class SpeedHypixelBHop(override val parent: ChoiceConfigurable<*>) : Choice("HypixelBHop") {
    companion object {
        /**
         * Vanilla maximum speed
         * w/o: 0.2857671997172534
         * w/ Speed 1: 0.2919055664000211
         * w/ Speed 2: 0.2999088445964323
         *
         * Speed mod: 0.008003278196411223
         */
        private const val AT_LEAST = 0.281
        private const val SPEED_EFFECT_CONST = 0.13

    }

    private var wasFlagged = false
    private var airTicks = 0

    val repeatable = repeatable {
        if (player.isOnGround) {
            player.strafe()
            airTicks = 0
            return@repeatable
        } else {
            airTicks++
            if (airTicks == 1) {
                player.strafe(strength = 1.0)
            }
            if(airTicks == 5) {
                player.velocity.y = -0.1523351824467155
            }
        }
    }

    val jumpEvent = handler<PlayerJumpEvent> {
        val atLeast = if (!wasFlagged) {
            AT_LEAST + SPEED_EFFECT_CONST * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
        } else {
            0.0
        }

        player.strafe(speed = player.sqrtSpeed.coerceAtLeast(atLeast) - 0.002)
    }

    val moveHandler = handler<MovementInputEvent> {
        if (!player.isOnGround || !player.moving) {
            return@handler
        }

        if (ModuleSpeed.shouldDelayJump())
            return@handler

        it.jumping = true
    }

    override fun disable() {
        wasFlagged = false
        airTicks = 0
    }

}
