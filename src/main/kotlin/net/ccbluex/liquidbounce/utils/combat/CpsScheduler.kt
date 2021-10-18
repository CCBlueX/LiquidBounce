/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

/**
 * A CPS scheduler
 *
 * Minecraft is counting every click until it they handle all inputs.
 * code:
 * while(this.options.keyAttack.wasPressed()) {
 *     this.doAttack();
 * }
 */
class CpsScheduler {

    var lastClick = 0L
    var clickTime = -1L

    fun clicks(condition: () -> Boolean, cps: IntRange): Int {
        val currTime = System.currentTimeMillis()

        var timeLeft = currTime - lastClick
        if (timeLeft > 1000) {
            lastClick = System.currentTimeMillis()
            return 0
        }
        if (clickTime < 0) {
            clickTime = clickTime(cps)
        }

        var clicks = 0

        while (timeLeft - clickTime > 0 && condition()) {
            timeLeft -= clickTime
            clicks++

            clickTime = clickTime(cps)
            lastClick = System.currentTimeMillis()
        }

        return clicks
    }

    // TODO: Make more stamina like
    private fun clickTime(cps: IntRange) =
        ((Math.random() * (1000 / cps.first - 1000 / cps.last + 1)) + 1000 / cps.last).toLong()

}
