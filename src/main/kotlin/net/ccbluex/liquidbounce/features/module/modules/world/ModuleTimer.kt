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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * Timer module
 *
 * Changes the speed of the entire game.
 */
object ModuleTimer : Module("Timer", Category.WORLD, disableOnQuit = true) {

    private val normalSpeed: Float by float("NormalSpeed", 0.5f, 0.1f..10f)
    private val normalSpeedTicks by int("NormalSpeedTicks", 20, 1..500)
    private val boostSpeed by float("BoostSpeed", 2f, 0.1f..10f)
    private val boostSpeedTicks by int("BoostSpeedTicks", 20, 1..500)
    private val onMove by boolean("OnMove", false)
    private var currentTimerState: TimerState = TimerState.NormalSpeed

    val repeatable = repeatable {
        if (onMove && !player.moving) {
            return@repeatable
        }

        when (currentTimerState) {
            TimerState.NormalSpeed -> {
                Timer.requestTimerSpeed(
                    normalSpeed, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleTimer, resetAfterTicks = normalSpeedTicks
                )
                waitTicks(normalSpeedTicks)
                currentTimerState = TimerState.BoostSpeed
            }

            TimerState.BoostSpeed -> {
                Timer.requestTimerSpeed(
                    boostSpeed, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleTimer, resetAfterTicks = boostSpeedTicks
                )
                waitTicks(boostSpeedTicks)
                currentTimerState = TimerState.NormalSpeed
            }
        }

        return@repeatable
    }

    override fun disable() {
        currentTimerState = TimerState.NormalSpeed
        Timer.requestTimerSpeed(1f, Priority.NOT_IMPORTANT, this@ModuleTimer)
    }

    enum class TimerState {
        NormalSpeed, BoostSpeed
    }
}
