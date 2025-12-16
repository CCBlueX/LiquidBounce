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
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot

import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot.NotebotStage
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer

object NotebotRenderer : PlacementRenderer("Render", true, ModuleNotebot) {
    private const val TRANSITION_TIME = 300L

    val testColor by color("TestColor", Color4b.RED.with(a = 90))
    val outlineTestColor by color("TestOutlineColor", Color4b.RED)
    val tuneColor by color("TuneColor", Color4b.YELLOW.with(a = 90))
    val outlineTuneColor by color("TuneOutlineColor", Color4b.YELLOW)

    private val stageChangeChronometer = Chronometer()

    private var currentStage = NotebotStage.TEST
    private var lastStage = NotebotStage.TEST

    override fun getColor(id: Int): Color4b {
        return lastStage.blockColor().interpolateTo(currentStage.blockColor(), getTransitionProgress())
    }

    override fun getOutlineColor(id: Int): Color4b {
        return lastStage.blockOutlineColor().interpolateTo(currentStage.blockOutlineColor(), getTransitionProgress())
    }

    fun onStateChange(stage: NotebotStage) {
        // Only change the target color if the animation was not finished to prevent sudden color changes.
        if (!stageChangeChronometer.hasElapsed(TRANSITION_TIME)) {
            this.currentStage = stage

            return
        }

        this.lastStage = this.currentStage
        this.currentStage = stage

        stageChangeChronometer.reset()
    }

    private fun getTransitionProgress(): Double {
        return (stageChangeChronometer.elapsed / TRANSITION_TIME.toDouble()).coerceAtMost(1.0)
    }

    fun reset() {
        this.currentStage = NotebotStage.TEST
        this.lastStage = NotebotStage.TEST

        this.clearSilently()
    }
}
