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
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.Collections
import java.util.IdentityHashMap

/** Where a task looks for its model; files trained for other inputs or outputs do not fit. */
@UnstableAddonApi
class ModelSlot(val task: String, val variant: String, val input: InputSchema, val outputs: Int) {
    val id get() = "$task/$variant"
    val resource get() = "/resources/liquidbounce/deeplearning/$task/$variant.${ModelFile.EXTENSION}"

    fun accepts(file: ModelFile) = file.task == task && file.variant == variant &&
        file.input.schema == input.schema && file.input.version == input.version &&
        file.input.size == input.size && file.network.outputs == outputs

    override fun toString() = id
}

/**
 * The model each slot uses: one an add-on installed at runtime, otherwise the one bundled with the
 * client. Installs are not saved; whoever installs a model does so again after a restart. A model that
 * fails to load or to predict is logged once and skipped until it is installed again.
 */
@UnstableAddonApi
object ModelRegistry {
    private val installed = HashMap<String, ModelFile>()
    private val bundled = HashMap<String, ModelFile?>()
    private val loaded = HashMap<String, LoadedModel>()
    private val failed = Collections.newSetFromMap(IdentityHashMap<ModelFile, Boolean>())
    private val lock = Any()

    fun active(slot: ModelSlot): ModelFile? = synchronized(lock) { installed[slot.id] ?: bundled(slot) }

    fun installed(slot: ModelSlot): ModelFile? = synchronized(lock) { installed[slot.id] }

    fun failed(slot: ModelSlot): Boolean = synchronized(lock) { active(slot)?.let { it in failed } == true }

    /** Runs [block] with the slot's model, or returns null while the engine or a working model is missing. */
    fun <T> use(slot: ModelSlot, block: (LoadedModel) -> T): T? = synchronized(lock) {
        if (!DeepLearningEngine.isInitialized) {
            return null
        }
        val file = active(slot)?.takeUnless { it in failed } ?: return null
        val model = loaded[slot.id]?.takeIf { it.file === file } ?: load(slot, file) ?: return null
        runCatching { block(model) }.getOrElse { throwable ->
            fail(slot, file, "Model ${file.name} failed for $slot", throwable)
            null
        }
    }

    fun install(slot: ModelSlot, file: ModelFile) {
        require(slot.accepts(file)) { "${file.task}/${file.variant} ${file.name} does not fit $slot" }
        synchronized(lock) {
            installed.put(slot.id, file)?.let(failed::remove)
            failed.remove(file)
            loaded.remove(slot.id)?.close()
        }
    }

    fun uninstall(slot: ModelSlot) = synchronized(lock) {
        installed.remove(slot.id)?.let(failed::remove)
        loaded.remove(slot.id)?.close()
    }

    fun close() = synchronized(lock) {
        loaded.values.forEach(LoadedModel::close)
        loaded.clear()
        failed.clear()
    }

    private fun load(slot: ModelSlot, file: ModelFile): LoadedModel? = runCatching { LoadedModel(file) }
        .onFailure { fail(slot, file, "Failed to load model ${file.name} for $slot", it) }
        .getOrNull()
        ?.also { loaded.put(slot.id, it)?.close() }

    private fun fail(slot: ModelSlot, file: ModelFile, message: String, throwable: Throwable) {
        failed += file
        loaded.remove(slot.id)?.close()
        logger.error(message, throwable)
    }

    private fun bundled(slot: ModelSlot): ModelFile? = bundled.getOrPut(slot.id) {
        runCatching { javaClass.getResourceAsStream(slot.resource)?.use(ModelFile::read) }
            .onFailure { logger.error("Failed to read the bundled model for $slot", it) }
            .getOrNull()
            ?.takeIf { file -> slot.accepts(file).also { if (!it) logger.error("Bundled model does not fit $slot") } }
    }
}
