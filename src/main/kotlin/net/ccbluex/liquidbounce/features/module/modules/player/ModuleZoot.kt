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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.effect.StatusEffects

/**
 * Zoot module
 *
 * Accelerate game time to stop bad effects and fire faster.
 */
object ModuleZoot : Module("Zoot", Category.PLAYER) {

    private val timer by float("Timer", 1f, 0.1f..10f)
    private val badEffects by boolean("BadEffects", true)
    private val fire by boolean("Fire", true)

    private val notInTheAir by boolean("NotInTheAir", true)
    private val notDuringMove by boolean("NotDuringMove", false)
    private val notDuringRegeneration by boolean("NotDuringRegeneration", false)

    private val packetType by enumChoice("PacketType", MovePacketType.FULL)

    val repeatable = repeatable {
        if (notInTheAir && !player.isOnGround) {
            return@repeatable
        }

        if (player.abilities.creativeMode || player.isDead) {
            return@repeatable
        }

        if (notDuringMove && player.moving) {
            return@repeatable
        }

        if (notDuringRegeneration && player.hasStatusEffect(StatusEffects.REGENERATION)) {
            return@repeatable
        }

        if (fire && player.isOnFire) {
            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleZoot)

            // Accelerate game time (1.8.X)
            repeat(9) {
                network.sendPacket(packetType.generatePacket())
            }
            return@repeatable
        }

        if (badEffects) {
            val (effect, status) = player.activeStatusEffects.maxByOrNull { it.value.duration }
                ?: return@repeatable

            if (!effect.value().isBeneficial && !status.isInfinite) {
                Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleZoot)

                // Accelerate game time (1.8.X)
                repeat(status.duration / 20) {
                    network.sendPacket(packetType.generatePacket())
                }
            }
        }
    }
}
