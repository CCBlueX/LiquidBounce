/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.utils.clicking

import it.unimi.dsi.fastutil.longs.LongList
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import java.util.Random

/** How a [Clicker] spaces its presses, with the rate [ClickPlan] plans at. */
abstract class ClickTechnique(name: String) : Mode(name), ClickTiming {
    abstract val cps: IntRange
}

class HumanClickTechnique(override val parent: ModeValueGroup<*>, maxCps: Int) : ClickTechnique("Human") {
    override val cps by intRange("CPS", 11..14, 1..maxCps, "clicks")
    private val timing = HumanClickTiming()

    override fun nextInterval(recent: LongList, comboMs: Long, cps: IntRange, random: Random) =
        timing.nextInterval(recent, comboMs, cps, random)
}

/**
 * Evenly spaced at the top of the CPS range, for anticheats that only look at the time since the last attack.
 */
class ConstantClickTechnique(override val parent: ModeValueGroup<*>, maxCps: Int) : ClickTechnique("Constant") {
    override val cps by intRange("CPS", 11..14, 1..maxCps, "clicks")

    override fun nextInterval(recent: LongList, comboMs: Long, cps: IntRange, random: Random) =
        ConstantClickTiming.nextInterval(recent, comboMs, cps, random)
}
