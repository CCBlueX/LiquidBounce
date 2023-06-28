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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.KeyBindingEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import kotlin.math.roundToInt

/**
 * ClickRecorder module
 *
 * Records your clicks which then can be used by cps-utilizing modules.
 */

object ModuleClickRecorder : Module("ClickRecorder", Category.MISC) {

    private val reset by boolean("Reset", false).listen {
        clickList.clear()

        chat("Cleared click list")
        return@listen false
    }

    private val startFromZero by boolean("StartFromZero", false).listen {
        progression = 0

        chat("Progression is now at zero.")
        return@listen false
    }

    private val ignoreInstructions by boolean("IgnoreInstructions", false)

    private val clickList = arrayListOf<Long>()

    private var started = false
    private var prevTime = -1L

    private var progression = 0
    private var lastClick = -1L

    override fun enable() {
        if (!ignoreInstructions) {
            chat("|------- ClickRecorder -------|")
            chat("Click to start")
            chat("Disable the module to stop")
            chat("Click on the Reset option if you wish to clear the click list")
            chat("Click on the StartFromZero option if you want ClickRecorder to start from zero")
            chat("|------- ClickRecorder -------|")
        }

        started = false
        prevTime = -1L
    }

    override fun disable() {
        if (started) {
            started = false

            chat("Stopped!")
        }
    }

    val keyBindingHandler = handler<KeyBindingEvent> {
        if (it.key != mc.options.attackKey) {
            return@handler
        }

        if (!started) {
            started = true
            prevTime = System.currentTimeMillis()

            chat("Started!")
            return@handler
        }

        repeat(it.key.timesPressed) {
            clickList.add(System.currentTimeMillis() - prevTime)

            prevTime = System.currentTimeMillis()
        }
    }

    fun getProgression(): Int {
        progression = progression.coerceAtMost(clickList.lastIndex)

        return progression
    }

    fun increaseProgression() {
        val inc = progression + 1

        if (inc > clickList.lastIndex) {
            progression = 0
            return
        }

        progression = inc
    }

    fun doClicks(condition: () -> Boolean): Int {
        var timeLeft = System.currentTimeMillis() - lastClick
        var clicks = 0

        if (started && enabled || clickList.isEmpty()) {
            return 0
        }

        if (lastClick == -1L || ((timeLeft - clickList[getProgression()]) / 50.0).roundToInt() * 50 > 50) {
            lastClick = System.currentTimeMillis()

            return if (condition()) 1 else 0
        }

        while (timeLeft >= clickList[getProgression()] && condition()) {
            timeLeft -= clickList[getProgression()]
            clicks++

            lastClick = System.currentTimeMillis()
            increaseProgression()
        }

        return clicks
    }
}
