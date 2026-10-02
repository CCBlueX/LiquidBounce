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
import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.io.FastByteArrayInputStream
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.listener.OverlayTrainingListener
import net.ccbluex.liquidbounce.deeplearn.model.InputNormalization
import net.ccbluex.liquidbounce.deeplearn.model.InputSchema
import net.ccbluex.liquidbounce.deeplearn.model.LoadedModel
import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import net.ccbluex.liquidbounce.deeplearn.model.NetworkSpec
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Locale
import kotlin.math.sqrt

private const val NUM_EPOCH = 100
private const val BATCH_SIZE = 32

private val NETWORK = NetworkSpec(listOf(128, 64, 32), 2, NetworkSpec.Activation.RELU)

/**
 * A model file in the client, bundled or trained here: predicts through [LoadedModel], trains with DJL from the
 * parameters it was loaded with.
 */
abstract class ModelWrapper(
    name: String,
    override val parent: ModeValueGroup<*>
) : Mode(name), Closeable {

    private var file: ModelFile? = null
    private var loaded: LoadedModel? = null
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

        val outputSize = NETWORK.outputs
        require(labels.size % outputSize == 0) { "Labels must contain $outputSize values per sample" }
        val sampleCount = labels.size / outputSize
        require(features.size % sampleCount == 0) { "Features must have the same sample count as labels" }
        val inputSize = features.size / sampleCount

        synchronized(lock) {
            check(!closed) { "Model '$name' is closed" }
            val base = file
            val normalization = base?.normalization ?: fitNormalization(features, inputSize)
            val normalized = FloatArray(features.size)
            val sample = FloatArray(inputSize)
            for (index in 0 until sampleCount) {
                normalization.apply(features.copyOfRange(index * inputSize, (index + 1) * inputSize), sample)
                sample.copyInto(normalized, index * inputSize)
            }

            val trainingConfig = DefaultTrainingConfig(Loss.l2Loss())
                .optInitializer(XavierInitializer(), "weight")
                .optOptimizer(
                    Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.001f))
                        .build()
                )
                .addTrainingListeners(LoggingTrainingListener(), OverlayTrainingListener(NUM_EPOCH))

            Model.newInstance(name).use { model ->
                model.block = NETWORK.block()
                base?.let {
                    DataInputStream(FastByteArrayInputStream(it.parameters)).use { stream ->
                        model.block.loadParameters(model.ndManager, stream)
                    }
                }
                model.newTrainer(trainingConfig).use { trainer ->
                    NDManager.newBaseManager().use { manager ->
                        val trainingSet = ArrayDataset.Builder()
                            .setData(manager.create(normalized, Shape(sampleCount.toLong(), inputSize.toLong())))
                            .optLabels(manager.create(labels, Shape(sampleCount.toLong(), outputSize.toLong())))
                            .setSampling(BATCH_SIZE, true)
                            .build()
                        trainer.initialize(Shape(BATCH_SIZE.toLong(), inputSize.toLong()))

                        EasyTrain.fit(trainer, NUM_EPOCH, trainingSet, null)
                    }
                }

                val parameters = ByteArrayOutputStream().also { bytes ->
                    DataOutputStream(bytes).use { model.block.saveParameters(it) }
                }.toByteArray()
                file = ModelFile("combat", "angle", name, InputSchema("combat-sample", 1, inputSize), NETWORK,
                    normalization, JsonObject(), "", parameters)
                loaded?.close()
                loaded = null
            }
        }
    }

    fun load(name: String = this.name) {
        val path = modelsFolder.resolve(name).resolve("model.${ModelFile.EXTENSION}").toPath()
        val file = if (path.toFile().exists()) {
            ModelFile.read(path)
        } else {
            val lowercaseName = name.lowercase(Locale.ENGLISH)
            javaClass.getResourceAsStream("/resources/liquidbounce/models/$lowercaseName.${ModelFile.EXTENSION}")!!
                .use(ModelFile::read)
        }

        synchronized(lock) {
            check(!closed) { "Model '$name' is closed" }
            this.file = file
            loaded?.close()
            loaded = null
        }
    }

    fun save(name: String = this.name) {
        checkNotNull(file) { "Model '$name' has nothing to save" }
            .write(modelsFolder.resolve(name).resolve("model.${ModelFile.EXTENSION}").toPath())
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
        }
    }

}

/** New models learn from inputs scaled to the recorded spread. */
private fun fitNormalization(features: FloatArray, size: Int): InputNormalization {
    val samples = features.size / size
    val mean = FloatArray(size) { input ->
        (0 until samples).sumOf { features[it * size + input].toDouble() }.toFloat() / samples
    }
    val scale = FloatArray(size) { input ->
        val variance = (0 until samples).sumOf {
            val deviation = (features[it * size + input] - mean[input]).toDouble()
            deviation * deviation
        }
        sqrt(variance / samples).toFloat().coerceAtLeast(1e-3f)
    }
    return InputNormalization(mean, scale)
}
