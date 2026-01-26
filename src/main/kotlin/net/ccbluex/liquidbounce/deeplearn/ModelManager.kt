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

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModel
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.time.measureTime

object ModelManager : EventListener, Configurable("AI") {

    /**
     * Base models that are always available
     * and are included in the LiquidBounce JAR.
     *
     * The name can contain uppercase characters,
     * but the file should always be lowercase.
     */
    val combatModels = arrayOf(
        "21KC11KP",
        "19KC8KP"
    )

    /**
     * Available models from the models folder
     */
    private val availableCombatModels: List<String>
        get() = modelsFolder
            .listFiles { file -> file.isDirectory }
            ?.map { file -> file.nameWithoutExtension } ?: emptyList()

    private val allCombatModels: Array<String>
        get() = combatModels + availableCombatModels

    val models = choices(this, "Model", 0) { choiceConfigurable ->
        // Empty models for start-up initialization.
        // These will be replaced later on at [load].
        allCombatModels.mapToArray { name ->
            TwoDimensionalRegressionModel(name, choiceConfigurable)
        }
    }

    /**
     * Load models from the models folder. This only has to be triggered
     * when reloading the models. Otherwise, the models are loaded on startup
     * through the choice initialization.
     */
    fun load() {
        logger.info("[AI] Loading models...")
        val choices = allCombatModels.mapToArray { name ->
            TwoDimensionalRegressionModel(name, models)
        }

        for (model in choices) {
            runCatching {
                measureTime {
                    model.load()
                }
            }.onFailure { error ->
                logger.error("[AI] Failed to load model '${model.name}'.", error)
            }.onSuccess { time ->
                logger.info("[AI] Loaded model '${model.name}' in ${time.inWholeMilliseconds}ms.")
            }
        }

        models.choices = choices.toMutableList()
        models.setByString(models.activeChoice.name)
        ModuleClickGui.sync()
    }

    /**
     * Unload all models.
     */
    fun unload() {
        models.choices.forEach { it.close() }
        models.choices.clear()
    }

    /**
     * Clear out all models and load-in the models again.
     */
    fun reload() {
        unload()
        load()
    }

}
