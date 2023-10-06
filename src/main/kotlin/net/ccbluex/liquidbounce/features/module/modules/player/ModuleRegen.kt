/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * Regen module
 *
 * Regenerates your health much faster.
 */

object ModuleRegen : Module("Regen", Category.PLAYER) {
    private val health by int("Health", 18, 0..20)
    private val speed by int("Speed", 100, 1..100)
    private val timer by float("Timer", 0.5f, 0.1f..10f)
    private val noAir by boolean("NoAir", false)
    private val potionEffect by boolean("PotionEffect", false)

    val repeatable = repeatable {
        if ((!noAir && player.isOnGround) && !player.abilities.creativeMode && player.health > 0 && player.health < health) {
            if (potionEffect && !player.hasStatusEffect(StatusEffects.REGENERATION)) {
                return@repeatable
            }

            if (player.hungerManager.foodLevel < 20) {
                return@repeatable
            }

            repeat(speed) {
                network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround))
            }

            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE)
        }
    }
}
