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

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    fun write(path: Path) = atomicWrite(path) { it.write(bytes()) }

    fun bytes(): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.entry(DESCRIPTION, GSON.toJson(description()).toByteArray())
            zip.entry(PARAMETERS, parameters)
        }
        return bytes.toByteArray()
    }

    private fun description() = JsonObject().apply {
        addProperty("format", FORMAT)
        addProperty("task", task)
        addProperty("variant", variant)
        addProperty("name", name)
        add("input", JsonObject().apply {
            addProperty("schema", input.schema)
            addProperty("version", input.version)
            addProperty("size", input.size)
        })
        add("network", JsonObject().apply {
            addProperty("type", "mlp")
            add("hidden", JsonArray().apply { network.hidden.forEach(::add) })
            addProperty("activation", network.activation)
            addProperty("outputs", network.outputs)
        })
        addProperty("provenance", provenance)
        add("metadata", metadata)
        add("normalization", JsonObject().apply {
            add("limit", float(normalization.limit))
            add("mean", floats(normalization.mean))
            add("scale", floats(normalization.scale))
        })
    }

    companion object {
        const val EXTENSION = "lbmodel"
        const val FORMAT = 1
        private const val DESCRIPTION = "model.json"
        private const val PARAMETERS = "parameters.bin"
        private const val MAX_DESCRIPTION = 1 shl 20
        private const val MAX_PARAMETERS = 16 shl 20
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        // Zip entries otherwise carry the write time, so the same model would never give the same bytes
        private val ENTRY_TIME = LocalDateTime.of(1980, 1, 1, 0, 0)

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
                    network["activation"].asString),
                InputNormalization(floats(normalization["mean"]), floats(normalization["scale"]),
                    float(normalization["limit"])),
                json["metadata"]?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject(),
                json["provenance"]?.asString.orEmpty(),
                parameters,
            )
        }

        /** Finite floats as numbers, the rest as strings, which JSON has no number for. */
        fun float(value: Float): JsonPrimitive =
            if (value.isFinite()) JsonPrimitive(value) else JsonPrimitive(value.toString())

        fun float(element: JsonElement): Float = element.asString.toFloat()

        fun floats(values: FloatArray) = JsonArray(values.size).apply { values.forEach { add(float(it)) } }

        fun floats(element: JsonElement): FloatArray =
            element.asJsonArray.let { array -> FloatArray(array.size()) { float(array[it]) } }

        private fun ZipOutputStream.entry(name: String, content: ByteArray) {
            putNextEntry(ZipEntry(name).apply { setTimeLocal(ENTRY_TIME) })
            write(content)
            closeEntry()
        }

        private fun ZipInputStream.readLimited(limit: Int): ByteArray {
            val bytes = readNBytes(limit + 1)
            require(bytes.size <= limit) { "Model entry too large" }
            return bytes
        }
    }
}

/** Writes through a temporary file, so a crash never leaves half a file behind. */
@UnstableAddonApi
fun atomicWrite(path: Path, compressed: Boolean = false, write: (DataOutputStream) -> Unit) {
    Files.createDirectories(path.toAbsolutePath().parent)
    val temporary = Files.createTempFile(path.toAbsolutePath().parent, path.fileName.toString(), ".tmp")
    try {
        val stream = Files.newOutputStream(temporary).buffered()
        DataOutputStream(if (compressed) GZIPOutputStream(stream) else stream).use(write)
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(temporary)
    }
}
