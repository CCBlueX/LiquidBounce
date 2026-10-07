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
import net.ccbluex.liquidbounce.deeplearn.clicking.ClickingModels
import net.ccbluex.liquidbounce.deeplearn.clicking.ClickingStyle
import net.ccbluex.liquidbounce.deeplearn.combat.CombatModels
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.deeplearn.model.ModelStatus
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import org.apache.logging.log4j.LogManager

/**
 * Every model bundled with the client loads in the engine and predicts.
 */
class BundledModelsGameTest : FabricClientGameTest {
    private val logger = LogManager.getLogger("BundledModelsGameTest")

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        check(context.fromClient { DeepLearningEngine.isInitialized }) { "The deep learning engine did not load" }
        val slots = ClickingStyle.entries.map(ClickingModels::slot) + CombatModels.SLOT
        context.onClient {
            for (slot in slots) {
                for (name in slot.bundled) {
                    val outputs = ModelRegistry.use(slot, name) { it.predict(FloatArray(slot.input.size)).values }
                    check(outputs?.size == slot.outputs) { "$slot $name does not predict" }
                    check(ModelRegistry.status(slot, name) == ModelStatus.READY) { "$slot $name is not ready" }
                }
            }
        }
        logger.info("PASS: {} bundled models predict", slots.sumOf { it.bundled.size })
    }
}
