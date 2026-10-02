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
package net.ccbluex.liquidbounce.deeplearn.models

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.deeplearn.model.InputNormalization
import net.ccbluex.liquidbounce.deeplearn.model.InputSchema
import net.ccbluex.liquidbounce.deeplearn.model.LoadedModel
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import net.ccbluex.liquidbounce.deeplearn.model.NetworkSpec
import java.io.DataInputStream
import java.io.InputStream

/**
 * The models from before [ModelFile], `19kc8kp.params` and `21kc11kp.params`, in DJL's own parameter files. Read
 * without the engine, they predict through [LoadedModel] like any other.
 */
object LegacyModelFile {
    val NETWORK = NetworkSpec(listOf(128, 64, 32), 2, NetworkSpec.Activation.BATCH_NORM_RELU)

    // "DJL@", version, model name, data type, input names and shapes, properties, then the block's parameters
    fun read(stream: InputStream, name: String): ModelFile {
        val input = DataInputStream(stream)
        require(input.readNBytes(4).decodeToString() == "DJL@") { "$name is not a DJL parameter file" }
        require(input.readInt() == 1) { "Unsupported parameter file version in $name" }
        input.readUTF()
        require(input.readUTF() == "FLOAT32") { "$name does not hold FLOAT32 parameters" }
        var size = 0
        repeat(input.readInt()) {
            input.readUTF()
            val shape = LongArray(input.readInt()) { input.readLong() }
            repeat(input.readInt()) { input.readChar() }
            size = shape.last().toInt()
        }
        repeat(input.readInt()) {
            input.readUTF()
            input.readUTF()
        }
        return ModelFile(
            "combat", "legacy", name, InputSchema("combat-legacy", 1, size), NETWORK,
            InputNormalization(FloatArray(size), FloatArray(size) { 1f }, Float.POSITIVE_INFINITY), JsonObject(), "",
            input.readAllBytes(),
        )
    }
}
