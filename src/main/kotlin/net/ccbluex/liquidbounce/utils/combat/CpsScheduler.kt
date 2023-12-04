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

package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleClickRecorder
import kotlin.math.roundToInt

/**
 * A CPS scheduler
 *
 * Minecraft is counting every click until it handles all inputs.
 * code:
 * while(this.options.keyAttack.wasPressed()) {
 *     this.doAttack();
 * }
 */
class CpsScheduler : Configurable("CpsScheduler") {

    private val useClickRecorder by boolean("UseClickRecorder", false)

    private var lastClick = 0L
    private var clickTime = -1L

    companion object {
        const val MINECRAFT_TIME_MS = 50L
    }

    /**
     * Calculates if the next click is on the next tick or not.
     * Allows to predict future actions and behave accordingly.
     */
    fun isClickOnNextTick(ticks: Int = 1) = clickTime != -1L
        && (System.currentTimeMillis() - lastClick + (MINECRAFT_TIME_MS * ticks)) >= clickTime

    fun clicks(condition: () -> Boolean, cps: IntRange): Int {
        var timeLeft = System.currentTimeMillis() - lastClick
        var clicks = 0

        if (useClickRecorder) {
            return ModuleClickRecorder.doClicks(condition)
        }

        // Does the clickTime need a forced update or are we a tick late?
        if (clickTime == -1L || ((timeLeft - clickTime) / 50.0).roundToInt() * 50 > 50) {
            clickTime = clickTime(cps)
            lastClick = System.currentTimeMillis()

            return if (condition()) 1 else 0
        }

        while (timeLeft >= clickTime && condition()) {
            timeLeft -= clickTime
            clicks++

            clickTime = clickTime(cps)
            lastClick = System.currentTimeMillis()
        }

        return clicks
    }

    // TODO: Make more stamina like
    private fun clickTime(cps: IntRange) = 1000L / cps.random()

}
