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
package net.ccbluex.liquidbounce.deeplearn.models

import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import ai.djl.nn.Activation
import ai.djl.nn.Blocks
import ai.djl.nn.SequentialBlock
import ai.djl.nn.core.Linear
import ai.djl.nn.norm.BatchNorm
import ai.djl.training.DefaultTrainingConfig
import ai.djl.training.EasyTrain
import ai.djl.training.dataset.ArrayDataset
import ai.djl.training.initializer.XavierInitializer
import ai.djl.training.listener.LoggingTrainingListener
import ai.djl.training.loss.Loss
import ai.djl.training.optimizer.Adam
import ai.djl.training.tracker.Tracker
import ai.djl.translate.TranslateException
import ai.djl.translate.Translator
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.listener.OverlayTrainingListener
import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path
import java.util.*

private const val NUM_EPOCH = 100
private const val BATCH_SIZE = 32

abstract class ModelWrapper<I, O>(
    name: String,
    val translator: Translator<I, O>,
    val outputs: Long,
    override val parent: ChoiceConfigurable<*>
) : Choice(name), Closeable {

    private val model: Model by lazy {
        Model.newInstance(name).apply {
            block = createMlpBlock(outputs)
        }
    }
    private val predictor: Predictor<I, O> by lazy { model.newPredictor(translator) }

    @Throws(TranslateException::class)
    fun predict(input: I): O {
        require(DeepLearningEngine.isInitialized) { "DeepLearningEngine is not initialized" }

        return predictor.predict(input)
    }

    fun train(features: Array<FloatArray>, labels: Array<FloatArray>) {
        require(DeepLearningEngine.isInitialized) { "DeepLearningEngine is not initialized" }

        require(features.size == labels.size) { "Features and labels must have the same size" }
        require(features.isNotEmpty()) { "Features and labels must not be empty" }
        val inputs = features[0].size.toLong()

        val trainingConfig = DefaultTrainingConfig(Loss.l2Loss())
            .optInitializer(XavierInitializer(), "weight")
            .optOptimizer(
                Adam.builder()
                    .optLearningRateTracker(Tracker.fixed(0.001f))
                    .build()
            )
            .addTrainingListeners(LoggingTrainingListener(), OverlayTrainingListener(NUM_EPOCH))
        val trainer = model.newTrainer(trainingConfig)

        val manager = NDManager.newBaseManager()
        val trainingSet = ArrayDataset.Builder()
            .setData(manager.create(features))
            .optLabels(manager.create(labels))
            .setSampling(BATCH_SIZE, true)
            .build()
        trainer.initialize(Shape(BATCH_SIZE.toLong(), inputs))

        EasyTrain.fit(trainer, NUM_EPOCH, trainingSet, null)
    }

    fun load(stream: InputStream) {
        model.load(stream)
    }

    fun load(path: Path) {
        model.load(path, "tf")
    }

    fun load(name: String = this.name) {
        val folder = modelsFolder.resolve(name)

        if (folder.exists()) {
            load(folder.toPath())
        } else {
            val lowercaseName = name.lowercase(Locale.ENGLISH)
            javaClass.getResourceAsStream("/resources/liquidbounce/models/${lowercaseName}.params")!!.use { stream ->
                load(stream)
            }
        }
    }

    fun save(path: Path) {
        model.save(path, "tf")
    }

    fun save(name: String = this.name) {
        save(modelsFolder.resolve(name).toPath())
    }

    fun delete() {
        close()
        modelsFolder.resolve(name).delete()
    }

    override fun close() {
        predictor.close()
        model.close()
    }

}

/**
 * Create a block for the model. This is a simple Multi-Layer Perceptron (MLP) model.
 */
private fun createMlpBlock(outputs: Long) = SequentialBlock()
    .add(Linear.builder()
        .setUnits(128)
        .build())
    .add(Blocks.batchFlattenBlock())
    .add(BatchNorm.builder().build())
    .add(Activation.reluBlock())

    .add(Linear.builder()
        .setUnits(64)
        .build())
    .add(Blocks.batchFlattenBlock())
    .add(BatchNorm.builder().build())
    .add(Activation.reluBlock())

    .add(Linear.builder()
        .setUnits(32)
        .build())
    .add(Blocks.batchFlattenBlock())
    .add(BatchNorm.builder().build())
    .add(Activation.reluBlock())

    .add(Linear.builder()
        .setUnits(outputs)
        .build())
