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

package net.ccbluex.liquidbounce.deeplearn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModel
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.util.Locale
import kotlin.time.measureTime

object ModelManager : EventListener, ValueGroup("AI") {

    private val logger = clientLogger("AI/ModelManager")
    private val lifecycleMutex = Mutex()

    /**
     * Base models that are always available
     * and are included in the LiquidBounce JAR.
     *
     * The name can contain uppercase characters,
     * but the file should always be lowercase.
     *
     * A user model from the models folder with the same name
     * overrides the bundled model of the same name.
     */
    private val builtInCombatModels = arrayOf(
        "21KC11KP",
        "19KC8KP"
    )

    /**
     * Available models from the models folder
     */
    private val availableCombatModels: Array<out String>
        get() = modelsFolder.list { folder, name -> folder.resolve(name).isDirectory }.orEmpty()

    /**
     * Bundled models merged with user models from the models folder.
     * User models take precedence over bundled models with the same name,
     * so that improved copies of bundled models are used instead of the originals.
     */
    private val allCombatModels: Collection<String>
        get() = buildMap {
            availableCombatModels.forEach { name ->
                putIfAbsent(name.lowercase(Locale.ENGLISH), name)
            }
            builtInCombatModels.forEach { name ->
                putIfAbsent(name.lowercase(Locale.ENGLISH), name)
            }
        }.values

    val models = modes(this, "Model", 0) { modeValueGroup ->
        // Empty models for start-up initialization.
        // These will be replaced later on at [load].
        allCombatModels.mapToArray { name ->
            TwoDimensionalRegressionModel(name, modeValueGroup)
        }
    }

    /**
     * Load models from the models folder. This only has to be triggered
     * when reloading the models. Otherwise, the models are loaded on startup
     * through the choice initialization.
     */
    suspend fun load() = lifecycleMutex.withLock {
        logger.info("Loading models...")
        val activeModelName = withContext(MinecraftDispatcher) {
            models.activeMode.name
        }
        val choices = withContext(Dispatchers.IO) {
            loadModels()
        }

        val previousModels = withContext(MinecraftDispatcher) {
            val previousModels = models.modes.toTypedArray()

            runCatching {
                models.modes = choices.toMutableList()
                val nextModelName = choices.firstOrNull { model -> model.name == activeModelName }
                    ?.name ?: choices.first().name
                models.setByString(nextModelName)
                ModuleClickGui.sync()
                previousModels.asList()
            }.getOrElse { error ->
                // Roll back the swap, keeping the previous models active.
                logger.error("Failed to activate the newly loaded models, keeping the previous ones.", error)
                models.modes = previousModels.toMutableList()
                models.setByString(activeModelName)
                choices.forEach { model -> model.close() }
                emptyList()
            }
        }

        withContext(Dispatchers.IO) {
            previousModels.forEach { model -> model.close() }
        }
    }

    private fun loadModels(): List<TwoDimensionalRegressionModel> {
        val loadedModels = allCombatModels.mapNotNull { name ->
            val model = TwoDimensionalRegressionModel(name, models)

            runCatching {
                measureTime {
                    model.load()
                }
            }.onFailure { error ->
                logger.error("Failed to load model '${model.name}'.", error)
                model.close()
            }.onSuccess { time ->
                logger.info("Loaded model '${model.name}' in ${time.inWholeMilliseconds}ms.")
            }.getOrNull()?.let { model }
        }

        check(loadedModels.isNotEmpty()) { "No combat models could be loaded" }
        return loadedModels
    }

    /**
     * Clear out all models and load-in the models again.
     */
    suspend fun reload() = load()

}
