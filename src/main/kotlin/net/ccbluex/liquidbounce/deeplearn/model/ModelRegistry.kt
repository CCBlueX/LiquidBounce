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

import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Where a task looks for its model; files trained for other inputs or outputs do not fit. [bundled] names the
 * models shipped with the client, the first being the default.
 */
@UnstableAddonApi
class ModelSlot(
    val task: String,
    val variant: String,
    val input: InputSchema,
    val outputs: Int,
    val bundled: List<String>,
) {
    val id get() = "$task/$variant"

    fun resource(name: String) = "/resources/liquidbounce/deeplearning/$task/$variant/$name.${ModelFile.EXTENSION}"

    fun accepts(file: ModelFile) = file.task == task && file.variant == variant &&
        file.input.schema == input.schema && file.input.version == input.version &&
        file.input.size == input.size && file.network.outputs == outputs

    override fun toString() = id
}

@UnstableAddonApi
enum class ModelStatus(private val key: String) {
    READY("ready"),
    ENGINE_UNAVAILABLE("engineUnavailable"),
    MISSING("missing"),
    FAILED("failed");

    fun text() = translation("liquidbounce.deeplearning.status.$key")
}

/**
 * The model of each slot: one an add-on installed at runtime, otherwise the bundled one asked for. Installs are
 * not saved; whoever installs a model does so again after a restart. A model that fails to load or to predict is
 * logged once and skipped until it is installed again.
 */
@UnstableAddonApi
object ModelRegistry {
    private class Entry(val slot: ModelSlot) {
        var installed: ModelFile? = null
        val bundled = HashMap<String, ModelFile?>()
        var loaded: LoadedModel? = null
        val failed: MutableSet<ModelFile> = Collections.newSetFromMap(IdentityHashMap())

        fun file(name: String) = installed ?: bundled.getOrPut(name) { readBundled(slot, name) }

        fun unload() {
            loaded?.close()
            loaded = null
        }
    }

    private val entries = ConcurrentHashMap<String, Entry>()

    private fun <T> entry(slot: ModelSlot, block: Entry.() -> T): T =
        entries.computeIfAbsent(slot.id) { Entry(slot) }.let { synchronized(it) { it.block() } }

    /** The installed model, otherwise the bundled one called [name]. */
    fun file(slot: ModelSlot, name: String = slot.bundled.first()): ModelFile? = entry(slot) { file(name) }

    fun installed(slot: ModelSlot): ModelFile? = entry(slot) { installed }

    fun status(slot: ModelSlot, name: String = slot.bundled.first()): ModelStatus = entry(slot) {
        val file = file(name)
        when {
            !DeepLearningEngine.isInitialized -> ModelStatus.ENGINE_UNAVAILABLE
            file == null -> ModelStatus.MISSING
            file in failed -> ModelStatus.FAILED
            else -> ModelStatus.READY
        }
    }

    /** Runs [block] with the slot's model, or returns null unless [status] is [ModelStatus.READY]. */
    fun <T> use(slot: ModelSlot, name: String = slot.bundled.first(), block: (LoadedModel) -> T): T? = entry(slot) {
        val file = file(name)?.takeUnless { it in failed || !DeepLearningEngine.isInitialized } ?: return@entry null
        runCatching {
            val model = loaded?.takeIf { it.file === file } ?: LoadedModel(file).also {
                unload()
                loaded = it
            }
            block(model)
        }.getOrElse { throwable ->
            failed += file
            unload()
            logger.error("Model ${file.name} failed for $slot", throwable)
            null
        }
    }

    fun install(slot: ModelSlot, file: ModelFile) {
        require(slot.accepts(file)) { "${file.task}/${file.variant} ${file.name} does not fit $slot" }
        entry(slot) {
            installed = file
            failed.remove(file)
            unload()
        }
    }

    fun uninstall(slot: ModelSlot) = entry(slot) {
        installed = null
        unload()
    }

    private fun readBundled(slot: ModelSlot, name: String): ModelFile? =
        runCatching { javaClass.getResourceAsStream(slot.resource(name))?.use(ModelFile::read) }
            .onFailure { logger.error("Failed to read the bundled model $name for $slot", it) }
            .getOrNull()
            ?.takeIf { file ->
                slot.accepts(file).also { if (!it) logger.error("Bundled model $name does not fit $slot") }
            }
}
