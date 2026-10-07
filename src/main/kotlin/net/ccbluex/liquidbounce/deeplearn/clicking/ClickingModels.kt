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
package net.ccbluex.liquidbounce.deeplearn.clicking

import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.deeplearn.model.InputSchema
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.deeplearn.model.ModelSlot
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import kotlin.random.Random

@UnstableAddonApi
enum class ClickingStyle(val id: String, val model: String) {
    BUTTERFLY("butterfly", "butterfly-1"),
    JITTER("jitterclick", "jitterclick-1"),
}

/** The clicking task's slots, one per [ClickingStyle]. */
@UnstableAddonApi
object ClickingModels {
    const val TASK = "clicking"
    val INPUT = InputSchema(TASK, ClickingFeatures.VERSION, ClickingFeatures.SIZE)

    private const val DEFAULT_INTERVAL = 100f

    private val input = FloatArray(ClickingFeatures.SIZE)

    private val slots = enumMapOf<ClickingStyle, ModelSlot> {
        ModelSlot(TASK, it.id, INPUT, ClickingOutputs.BINS, listOf(it.model))
    }

    fun slot(style: ClickingStyle) = slots.getValue(style)

    /** The mean gap the model clicks at, in ms, to scale its rhythm to a configured CPS. */
    fun meanInterval(file: ModelFile) = file.metadata["meanInterval"]?.let(ModelFile::float) ?: DEFAULT_INTERVAL

    fun meanInterval(style: ClickingStyle) = ModelRegistry.file(slot(style))?.let(::meanInterval)

    /** The gap in ms after [intervals] (ms, oldest first) [burstMs] into a burst, or null without a working model. */
    fun nextInterval(style: ClickingStyle, intervals: List<Float>, burstMs: Float, random: Random): Float? {
        ClickingFeatures.write(intervals, burstMs, input)
        val output = ModelRegistry.use(slot(style)) { model -> model.predict(input).values } ?: return null
        return ClickingOutputs.sample(output, random)
    }

    fun status(style: ClickingStyle) = ModelRegistry.status(slot(style))
}
