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
package net.ccbluex.liquidbounce.deeplearn.models

import ai.djl.Model
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import ai.djl.training.DefaultTrainingConfig
import ai.djl.training.EasyTrain
import ai.djl.training.dataset.ArrayDataset
import ai.djl.training.initializer.XavierInitializer
import ai.djl.training.listener.LoggingTrainingListener
import ai.djl.training.loss.Loss
import ai.djl.training.optimizer.Adam
import ai.djl.training.tracker.Tracker
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.listener.OverlayTrainingListener
import net.ccbluex.liquidbounce.deeplearn.model.LoadedModel
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.DataInputStream
import java.util.Locale

private const val NUM_EPOCH = 100
private const val BATCH_SIZE = 32

/**
 * A model in the [LegacyModelFile] layout: predicts through [LoadedModel], trains with DJL from the parameters it
 * was loaded with.
 */
abstract class ModelWrapper(
    name: String,
    override val parent: ModeValueGroup<*>
) : Mode(name), Closeable {

    private var file: ModelFile? = null
    private var loaded: LoadedModel? = null
    private var trained: Model? = null
    private val lock = Any()

    @Volatile
    private var closed = false

    fun predict(input: FloatArray): FloatArray {
        require(DeepLearningEngine.isInitialized) { "DeepLearningEngine is not initialized" }

        return synchronized(lock) {
            check(!closed) { "Model '$name' is closed" }
            val model = loaded ?: LoadedModel(checkNotNull(file) { "Model '$name' is not loaded" })
                .also { loaded = it }
            model.predict(input).values
        }
    }

    fun train(features: FloatArray, labels: FloatArray) {
        require(DeepLearningEngine.isInitialized) { "DeepLearningEngine is not initialized" }

        require(features.isNotEmpty()) { "Features and labels must not be empty" }
        require(labels.isNotEmpty()) { "Features and labels must not be empty" }

        val outputSize = LegacyModelFile.NETWORK.outputs
        require(labels.size % outputSize == 0) { "Labels must contain $outputSize values per sample" }
        val sampleCount = labels.size / outputSize
        require(features.size % sampleCount == 0) { "Features must have the same sample count as labels" }
        val inputSize = features.size / sampleCount

        synchronized(lock) {
            check(!closed) { "Model '$name' is closed" }
            val model = Model.newInstance(name).apply { block = LegacyModelFile.NETWORK.block() }
            trained?.close()
            trained = model
            file?.let { file ->
                DataInputStream(ByteArrayInputStream(file.parameters)).use {
                    model.block.loadParameters(model.ndManager, it)
                }
            }

            val trainingConfig = DefaultTrainingConfig(Loss.l2Loss())
                .optInitializer(XavierInitializer(), "weight")
                .optOptimizer(
                    Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.001f))
                        .build()
                )
                .addTrainingListeners(LoggingTrainingListener(), OverlayTrainingListener(NUM_EPOCH))

            model.newTrainer(trainingConfig).use { trainer ->
                NDManager.newBaseManager().use { manager ->
                    val trainingSet = ArrayDataset.Builder()
                        .setData(manager.create(features, Shape(sampleCount.toLong(), inputSize.toLong())))
                        .optLabels(manager.create(labels, Shape(sampleCount.toLong(), outputSize.toLong())))
                        .setSampling(BATCH_SIZE, true)
                        .build()
                    trainer.initialize(Shape(BATCH_SIZE.toLong(), inputSize.toLong()))

                    EasyTrain.fit(trainer, NUM_EPOCH, trainingSet, null)
                }
            }
        }
    }

    fun load(name: String = this.name) {
        val folder = modelsFolder.resolve(name)
        val file = if (folder.exists()) {
            LegacyModelFile.read(LegacyModelFile.latest(folder.toPath()), name)
        } else {
            val lowercaseName = name.lowercase(Locale.ENGLISH)
            javaClass.getResourceAsStream("/resources/liquidbounce/models/${lowercaseName}.params")!!.use { stream ->
                LegacyModelFile.read(stream, name)
            }
        }

        synchronized(lock) {
            check(!closed) { "Model '$name' is closed" }
            this.file = file
            loaded?.close()
            loaded = null
        }
    }

    fun save(name: String = this.name) {
        checkNotNull(trained) { "Model '$name' is not trained" }.save(modelsFolder.resolve(name).toPath(), "tf")
    }

    fun delete() {
        val folder = modelsFolder.resolve(name)
        check(folder.isDirectory) { "Model '$name' is not a user model" }

        if (!folder.deleteRecursively()) {
            // The model may still hold file handles (e.g. on Windows), so close it
            // and retry once before reporting the deletion as failed.
            close()
            check(folder.deleteRecursively()) { "Failed to delete model '$name'" }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true

            loaded?.close()
            trained?.close()
        }
    }

}
