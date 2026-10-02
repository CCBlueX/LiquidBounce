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

import ai.djl.Model
import ai.djl.nn.Activation
import ai.djl.nn.Blocks
import ai.djl.nn.SequentialBlock
import ai.djl.nn.core.Linear
import ai.djl.nn.norm.BatchNorm
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelManager
import net.ccbluex.liquidbounce.deeplearn.model.LoadedModel
import net.ccbluex.liquidbounce.deeplearn.models.LegacyModelFile
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModel
import net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import org.apache.logging.log4j.LogManager
import kotlin.random.Random

/**
 * The bundled models from before the model files predict exactly as DJL loading them into their original network
 * does, and the AI angle smooth's model choices predict with them.
 */
class LegacyModelGameTest : FabricClientGameTest {
    private val logger = LogManager.getLogger("LegacyModelGameTest")

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        check(context.fromClient { DeepLearningEngine.isInitialized }) { "The deep learning engine did not load" }
        val random = Random(1)
        val inputs = List(INPUTS) {
            floatArrayOf(
                random.nextDouble(-180.0, 180.0).toFloat(), random.nextDouble(-90.0, 90.0).toFloat(),
                random.nextDouble(-30.0, 30.0).toFloat(), random.nextDouble(-30.0, 30.0).toFloat(),
                random.nextDouble(0.0, 1.0).toFloat(), random.nextDouble(0.0, 36.0).toFloat(),
            )
        }
        for (name in listOf("19kc8kp", "21kc11kp")) {
            val bytes = checkNotNull(javaClass.getResourceAsStream("/resources/liquidbounce/models/$name.params"))
                .use { it.readAllBytes() }
            val expected = Model.newInstance(name).use { model ->
                model.block = originalNetwork()
                model.load(bytes.inputStream())
                model.newPredictor(FloatArrayInAndOutTranslator).use { predictor -> inputs.map(predictor::predict) }
            }
            val actual = LoadedModel(LegacyModelFile.read(bytes.inputStream(), name)).use { model ->
                inputs.map { model.predict(it).values }
            }
            inputs.indices.forEach { check(expected[it].contentEquals(actual[it])) { "$name differs at input $it" } }
        }
        context.onClient {
            for (model in ModelManager.models.modes) {
                check(model.predict(inputs.first()).all(Float::isFinite)) { "${model.name} does not predict" }
            }
        }
        logger.info("PASS: the old models predict bit for bit as before")
        context.onClient { improve(inputs.first(), random) }
    }

    /** What `.models improve` does with recorded samples. */
    private fun improve(input: FloatArray, random: Random) {
        val name = "LegacyModelGameTest"
        val folder = DeepLearningEngine.modelsFolder.resolve(name)
        try {
            val before = TwoDimensionalRegressionModel(name, ModelManager.models).use { candidate ->
                candidate.load("19KC8KP")
                val before = candidate.predict(input)
                candidate.train(FloatArray(SAMPLES * 6) { random.nextFloat() },
                    FloatArray(SAMPLES * 2) { random.nextFloat() })
                candidate.save()
                before
            }
            val after = TwoDimensionalRegressionModel(name, ModelManager.models).use { saved ->
                saved.load()
                saved.predict(input)
            }
            check(after.all(Float::isFinite) && !after.contentEquals(before)) { "Training changed nothing: $after" }
        } finally {
            folder.deleteRecursively()
        }
        logger.info("PASS: an improved model saves and loads again")
    }

    /** The network the old models were trained in, as the client built it before [LegacyModelFile]. */
    private fun originalNetwork() = SequentialBlock().apply {
        for (units in longArrayOf(128, 64, 32)) {
            add(Linear.builder().setUnits(units).build())
            add(Blocks.batchFlattenBlock())
            add(BatchNorm.builder().build())
            add(Activation.reluBlock())
        }
        add(Linear.builder().setUnits(2).build())
    }

    private companion object {
        const val INPUTS = 2000
        const val SAMPLES = 64
    }
}
