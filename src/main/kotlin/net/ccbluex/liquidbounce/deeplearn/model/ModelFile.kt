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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

/** What a model reads: a named feature [schema] at a [version], [size] values per decision. */
@UnstableAddonApi
class InputSchema(val schema: String, val version: Int, val size: Int)

/**
 * A trained model as a zip: `model.json`, readable as is, describes the task, input, network and
 * normalization, and `parameters.bin` holds the weights. [metadata] belongs to the task.
 */
@UnstableAddonApi
@Suppress("LongParameterList")
class ModelFile(
    val task: String,
    val variant: String,
    val name: String,
    val input: InputSchema,
    val network: NetworkSpec,
    val normalization: InputNormalization,
    val metadata: JsonObject,
    val provenance: String,
    val parameters: ByteArray,
) {
    init {
        require(normalization.mean.size == input.size) { "Normalization does not match the input" }
    }

    companion object {
        const val EXTENSION = "lbmodel"
        const val FORMAT = 1
        const val DESCRIPTION = "model.json"
        const val PARAMETERS = "parameters.bin"
        private const val MAX_DESCRIPTION = 1 shl 20
        private const val MAX_PARAMETERS = 16 shl 20

        fun read(path: Path): ModelFile = Files.newInputStream(path).use(::read)

        fun read(stream: InputStream): ModelFile {
            var description: JsonObject? = null
            var parameters: ByteArray? = null
            ZipInputStream(stream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        DESCRIPTION -> description = JsonParser.parseString(
                            zip.readLimited(MAX_DESCRIPTION).decodeToString()
                        ).asJsonObject
                        PARAMETERS -> parameters = zip.readLimited(MAX_PARAMETERS)
                    }
                }
            }
            return parse(checkNotNull(description) { "Model has no $DESCRIPTION" },
                checkNotNull(parameters) { "Model has no $PARAMETERS" })
        }

        private fun parse(json: JsonObject, parameters: ByteArray): ModelFile {
            require(json["format"].asInt == FORMAT) { "Unsupported model format ${json["format"]}" }
            val input = json["input"].asJsonObject
            val network = json["network"].asJsonObject
            require(network["type"].asString == "mlp") { "Unsupported network ${network["type"]}" }
            val normalization = json["normalization"].asJsonObject
            return ModelFile(
                json["task"].asString, json["variant"].asString, json["name"].asString,
                InputSchema(input["schema"].asString, input["version"].asInt, input["size"].asInt),
                NetworkSpec(network["hidden"].asJsonArray.map { it.asInt }, network["outputs"].asInt,
                    NetworkSpec.Activation.of(network["activation"].asString)),
                InputNormalization(floats(normalization["mean"]), floats(normalization["scale"]),
                    float(normalization["limit"])),
                json["metadata"]?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject(),
                json["provenance"]?.asString.orEmpty(),
                parameters,
            )
        }

        /** Non-finite floats are strings, which JSON has no number for. */
        internal fun float(element: JsonElement): Float = element.asString.toFloat()

        private fun floats(element: JsonElement): FloatArray =
            element.asJsonArray.let { array -> FloatArray(array.size()) { float(array[it]) } }

        private fun ZipInputStream.readLimited(limit: Int): ByteArray {
            val bytes = readNBytes(limit + 1)
            require(bytes.size <= limit) { "Model entry too large" }
            return bytes
        }
    }
}
