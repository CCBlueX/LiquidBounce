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
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModel
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.clientLogger
import kotlin.time.measureTime

object ModelManager : EventListener, ValueGroup("AI") {

    private val logger = clientLogger("AI/ModelManager")

    /**
     * Models included in the LiquidBounce JAR.
     *
     * The name can contain uppercase characters,
     * but the file should always be lowercase.
     */
    private val builtInCombatModels = arrayOf(
        "21KC11KP",
        "19KC8KP"
    )

    val models = modes(this, "Model", 0) { modeValueGroup ->
        builtInCombatModels.mapToArray { name ->
            TwoDimensionalRegressionModel(name, modeValueGroup)
        }
    }

    fun load() {
        for (model in models.modes) {
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
    }

}
