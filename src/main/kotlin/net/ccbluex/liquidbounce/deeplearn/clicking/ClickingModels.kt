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

import net.ccbluex.liquidbounce.deeplearn.model.InputSchema
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.deeplearn.model.ModelSlot
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi

@UnstableAddonApi
enum class ClickingStyle(val id: String) {
    BUTTERFLY("butterfly"),
    JITTER("jitterclick"),
}

/** The clicking task's slots, one per [ClickingStyle]. */
@UnstableAddonApi
object ClickingModels {
    const val TASK = "clicking"
    val INPUT = InputSchema(TASK, ClickingFeatures.VERSION, ClickingFeatures.SIZE)

    private const val DEFAULT_INTERVAL = 100f

    private val slots = ClickingStyle.entries.associateWith { ModelSlot(TASK, it.id, INPUT, ClickingOutputs.BINS) }

    fun slot(style: ClickingStyle) = slots.getValue(style)

    /** The mean gap the model clicks at, in ms, to scale its rhythm to a configured CPS. */
    fun meanInterval(file: ModelFile) = file.metadata["meanInterval"]?.let(ModelFile::float) ?: DEFAULT_INTERVAL

    fun meanInterval(style: ClickingStyle) = ModelRegistry.active(slot(style))?.let(::meanInterval)

    fun rhythm(style: ClickingStyle) =
        ClickingRhythm { input -> ModelRegistry.use(slot(style)) { model -> model.predict(input).values } }
}
