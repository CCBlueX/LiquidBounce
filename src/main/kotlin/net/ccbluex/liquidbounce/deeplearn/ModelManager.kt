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
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModel
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.time.measureTime

object ModelManager : EventListener, ValueGroup("AI") {

    private val logger: Logger = LogManager.getLogger("$CLIENT_NAME/AI/ModelManager")

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
    fun load() {
        logger.info("Loading models...")
        val choices = allCombatModels.mapToArray { name ->
            TwoDimensionalRegressionModel(name, models)
        }

        for (model in choices) {
            runCatching {
                measureTime {
                    model.load()
                }
            }.onFailure { error ->
                logger.error("Failed to load model '${model.name}'.", error)
            }.onSuccess { time ->
                logger.info("Loaded model '${model.name}' in ${time.inWholeMilliseconds}ms.")
            }
        }

        models.modes = choices.toMutableList()
        models.setByString(models.activeMode.name)
        ModuleClickGui.sync()
    }

    /**
     * Unload all models.
     */
    fun unload() {
        models.modes.forEach { it.close() }
        models.modes.clear()
    }

    /**
     * Clear out all models and load-in the models again.
     */
    fun reload() {
        unload()
        load()
    }

}
