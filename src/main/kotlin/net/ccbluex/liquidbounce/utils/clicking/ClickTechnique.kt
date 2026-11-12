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
import net.ccbluex.liquidbounce.deeplearn.clicking.ClickingModels
import net.ccbluex.liquidbounce.deeplearn.clicking.ClickingStyle
import net.ccbluex.liquidbounce.deeplearn.model.ModelStatus
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import java.util.Random
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.asKotlinRandom

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
 * The rhythm of a clicking model, without a CPS of its own. Without the engine or the model it clicks like
 * [HumanClickTechnique] around the model's mean rate and says so once.
 */
class ModelClickTechnique(
    override val parent: ModeValueGroup<*>,
    private val style: ClickingStyle,
    name: String,
) : ClickTechnique(name) {
    private val fallback = HumanClickTiming()
    private var notified: ModelStatus? = null

    override val cps: IntRange
        get() = ClickingModels.meanInterval(style)?.let { (1000f / it).roundToInt() }?.let { it - 1..it + 1 }
            ?: 11..14

    override fun nextInterval(recent: LongList, comboMs: Long, cps: IntRange, random: Random): Long {
        val interval = ClickingModels.nextInterval(style, recent.map { it.toFloat() }, comboMs.toFloat(),
            random.asKotlinRandom())
        if (interval != null) {
            notified = null
            return interval.roundToLong()
        }
        val status = ClickingModels.status(style)
        if (notified != status) {
            notified = status
            chat(markAsError(translation("liquidbounce.clicker.messages.modelNotReady", name, status.text(), cps)))
        }
        return fallback.nextInterval(recent, comboMs, cps, random)
    }
}

/**
 * Evenly spaced at the top of the CPS range, for anticheats that only look at the time since the last attack.
 */
class ConstantClickTechnique(override val parent: ModeValueGroup<*>, maxCps: Int) : ClickTechnique("Constant") {
    override val cps by intRange("CPS", 11..14, 1..maxCps, "clicks")

    override fun nextInterval(recent: LongList, comboMs: Long, cps: IntRange, random: Random) =
        ConstantClickTiming.nextInterval(recent, comboMs, cps, random)
}
