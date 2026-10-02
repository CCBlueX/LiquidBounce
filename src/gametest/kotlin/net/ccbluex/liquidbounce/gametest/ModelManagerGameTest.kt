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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import org.apache.logging.log4j.LogManager

/**
 * The bundled models predict through the AI angle smooth's model choices.
 */
class ModelManagerGameTest : FabricClientGameTest {
    private val logger = LogManager.getLogger("ModelManagerGameTest")

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        check(context.fromClient { DeepLearningEngine.isInitialized }) { "The deep learning engine did not load" }
        val input = floatArrayOf(25f, -4f, 3f, 0.5f, 0.3f, 9f)
        context.onClient {
            check(ModelManager.models.modes.map { it.name }.toSet() == setOf("19KC8KP", "21KC11KP"))
            for (model in ModelManager.models.modes) {
                check(model.predict(input).all(Float::isFinite)) { "${model.name} does not predict" }
            }
        }
        logger.info("PASS: the bundled models predict")
    }
}
