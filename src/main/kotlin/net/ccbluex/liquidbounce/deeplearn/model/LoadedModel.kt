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
package net.ccbluex.liquidbounce.deeplearn.model

import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.ndarray.types.DataType
import ai.djl.ndarray.types.Shape
import net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.DataInputStream

@UnstableAddonApi
class ModelOutput(val values: FloatArray, val clamped: Boolean)

/** A [ModelFile] ready to predict. Not thread-safe. */
@UnstableAddonApi
class LoadedModel(val file: ModelFile) : Closeable {
    private val model = Model.newInstance(file.task).apply { block = file.network.block() }
    private val predictor: Predictor<FloatArray, FloatArray>
    private val normalized = FloatArray(file.input.size)

    init {
        try {
            model.block.initialize(model.ndManager, DataType.FLOAT32, Shape(1, file.input.size.toLong()))
            DataInputStream(ByteArrayInputStream(file.parameters)).use {
                model.block.loadParameters(model.ndManager, it)
            }
            predictor = model.newPredictor(FloatArrayInAndOutTranslator)
        } catch (throwable: Throwable) {
            model.close()
            throw throwable
        }
    }

    /** Non-finite outputs come from broken weights or normalization and never make a usable decision. */
    fun predict(input: FloatArray): ModelOutput {
        val clamped = file.normalization.apply(input, normalized)
        val values = predictor.predict(normalized)
        check(values.all(Float::isFinite)) { "Model ${file.name} produced a non-finite output" }
        return ModelOutput(values, clamped)
    }

    override fun close() {
        predictor.close()
        model.close()
    }
}
