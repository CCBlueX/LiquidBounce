/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.models.MinaraiModel
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import kotlin.time.measureTime

object ModelHolster : EventListener, Configurable("DeepLearning") {

    /**
     * Base models that are always available
     * and are included in the LiquidBounce JAR.
     *
     * The name can contain uppercase characters,
     * but the file should always be lowercase.
     */
    val baseModels = arrayOf(
        "21KC11KP",
        "19KC8KP"
    )

    /**
     * Available models from the models folder
     */
    private val availableModels: List<String>
        get() = modelsFolder
            .listFiles { file -> file.isDirectory }
            ?.map { file -> file.nameWithoutExtension } ?: emptyList()

    private val allModels: Array<String>
        get() = baseModels + availableModels

    val models = choices(this, "Model", 0) { choiceConfigurable ->
        // Empty models for start-up initialization.
        // These will be replaced later on at [load].
        allModels.mapArray { name ->
            MinaraiModel(name, choiceConfigurable)
        }
    }

    /**
     * Load models from the models folder. This only has to be triggered
     * when reloading the models. Otherwise, the models are loaded on startup
     * through the choice initialization.
     */
    fun load() {
        logger.info("[DeepLearning] Loading models...")
        val choices = allModels.map { name ->
            MinaraiModel(name, models)
        }

        for (model in choices) {
            runCatching {
                measureTime {
                    model.load()
                }
            }.onFailure { error ->
                logger.error("[DeepLearning] Failed to load model '${model.name}'.", error)
            }.onSuccess { time ->
                logger.info("[DeepLearning] Loaded model '${model.name}' in ${time.inWholeMilliseconds}ms.")
            }
        }

        models.choices = choices.toMutableList()
        models.setByString(models.activeChoice.name)
        ModuleClickGui.reload()
    }

    /**
     * Unload all models.
     */
    fun unload() {
        val iterator = models.choices.iterator()

        while (iterator.hasNext()) {
            val model = iterator.next()
            model.close()
            iterator.remove()
        }
    }

    /**
     * Clear out all models and load-in the models again.
     */
    fun reload() {
        unload()
        load()
    }

}
