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

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer

// TODO animate transition
object NotebotRenderer : PlacementRenderer("Render", true, ModuleNotebot) {

    private val testColor by color("TestColor", Color4b.RED.with(a = 90))
    private val outlineTestColor by color("TestOutlineColor", Color4b.RED)
    private val tuneColor by color("TuneColor", Color4b.YELLOW.with(a = 90))
    private val outlineTuneColor by color("TuneOutlineColor", Color4b.YELLOW)

    private val chronometer = Chronometer()

    override fun getColor(id: Int): Color4b {
        val state = getState()
        return when (state) {
            NotebotState.TEST -> testColor
            NotebotState.TUNE -> testColor.interpolateTo(tuneColor, getTransitionProgress())
            NotebotState.PLAY -> {
                if (ModuleNotebot.previousState == NotebotState.TEST) {
                    testColor
                } else {
                    tuneColor
                }.interpolateTo(super.getColor(id), getTransitionProgress())
            }
        }
    }

    override fun getOutlineColor(id: Int): Color4b {
        val state = getState()
        return when (state) {
            NotebotState.TEST -> outlineTestColor
            NotebotState.TUNE -> testColor.interpolateTo(outlineTuneColor, getTransitionProgress())
            NotebotState.PLAY -> {
                if (ModuleNotebot.previousState == NotebotState.TEST) {
                    outlineTestColor
                } else {
                    outlineTuneColor
                }.interpolateTo(super.getOutlineColor(id), getTransitionProgress())
            }
        }
    }

    fun indicateStateChange() {
        chronometer.reset()
    }

    private fun getTransitionProgress() = (chronometer.elapsed / 300.0).coerceAtMost(1.0)

    private fun getState(): NotebotState {
        return if (ModuleNotebot.enabled) {
            ModuleNotebot.state
        } else {
            ModuleNotebot.previousState
        }
    }

}
