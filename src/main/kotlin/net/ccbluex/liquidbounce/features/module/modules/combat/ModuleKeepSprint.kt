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

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.random.Random

/**
 * When hitting an entity, the player will keep sprinting
 */
object ModuleKeepSprint : ClientModule("KeepSprint", Category.COMBAT) {
    private val motion by floatRange("Motion", 100f..100f, 0f..100f, "%")
    private val motionWhenHurt by floatRange("MotionWhenHurt", 100f..100f, 0f..100f, "%")
    private val hurtTime by intRange("HurtTime", 1..10, 1..10)
    private val chance by float("Chance", 100f, 0f..100f, "%")

    // prevents getting slowed multiple times in a tick (without knockback item)
    var sprinting = false

    @Suppress("unused")
    private val postTickHandler = handler<PlayerPostTickEvent> {
        sprinting = player.isSprinting
    }

    fun getMotion(): Double {
        if (Random.nextFloat() * 100 > chance) {
            return 0.6
        }

        return when {
            player.hurtTime in hurtTime -> motionWhenHurt
            else -> motion
        }.random() / 100.0
    }
}
