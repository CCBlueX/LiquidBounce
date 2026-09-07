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
package net.ccbluex.liquidbounce.features.command.commands.deeplearn

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.ModelManager
import net.ccbluex.liquidbounce.deeplearn.ModelManager.models
import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModel
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdChainScope
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatTrainerRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import net.minecraft.util.Util
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

object CommandModels : CommandRegistrar {
    private val mutationMutex = Mutex()

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("models") {
            literal("create") {
                modelNameArgument { name ->
                    execSuspend { ctx ->
                        createModel(ctx.get(name))
                    }
                }
            }
            literal("improve") {
                modelNameArgument { name ->
                    execSuspend { ctx ->
                        improveModel(ctx.get(name))
                    }
                }
            }
            literal("delete") {
                modelNameArgument { name ->
                    execSuspend { ctx ->
                        deleteModel(ctx.get(name))
                    }
                }
            }
            literal("reload") {
                execSuspend {
                    reloadModel()
                }
            }
            literal("browse") {
                exec {
                    browseModel()
                }
            }
        }
    }

    private fun CmdLiteralScope.modelNameArgument(block: CmdChainScope.ArgContinuation<String>) =
        argument(
            "name",
            ClientStringArgumentType.word(),
            suggestions(strings = { models.modes.map { it.name } }),
            block,
        )

    private suspend fun CmdI18n.createModel(name: String) {
        mutationMutex.withLock {
            // Check if model exists
            if (models.modes.any { model -> model.name.equals(name, true) }) {
                throw CommandException(t("create.modelExists", name))
            }

            // Check if the name is a valid name
            if (name.contains(Regex("[^a-zA-Z0-9-]"))) {
                throw CommandException(t("create.invalidName"))
            }

            chat(t("create.trainingStart", name))
            withContext(Dispatchers.Default) {
                trainModel(name)
            }
        }
    }

    private suspend fun CmdI18n.improveModel(name: String) {
        mutationMutex.withLock {
            val model = models.modes.find { model -> model.name.equals(name, true) } ?:
                throw CommandException(t("improve.modelNotFound", name))

            chat(t("improve.trainingStart", name))
            withContext(Dispatchers.Default) {
                trainModel(name, model)
            }
        }
    }

    private suspend fun CmdI18n.deleteModel(name: String) {
        mutationMutex.withLock {
            val model = models.modes.find { model ->
                model.name.equals(name, true) && modelsFolder.resolve(model.name).isDirectory
            }

            if (model == null) {
                chat(markAsError(t("delete.modelNotFound", name)))
                return@withLock
            }

            val deleted = withContext(Dispatchers.IO) {
                runCatching { model.delete() }
                    .onFailure { error ->
                        logger.error("Failed to delete model '$name'.", error)
                        chat(markAsError(t("delete.modelDeleteFailed", name, error.localizedMessage)))
                    }
                    .isSuccess
            }
            if (!deleted) {
                runCatching {
                    ModelManager.reload()
                }.onFailure { error ->
                    logger.error("Failed to restore models after deleting '$name' failed.", error)
                }
                return@withLock
            }

            runCatching {
                ModelManager.reload()
            }.onFailure { error ->
                logger.error("Failed to reload models after deleting '$name'.", error)
                chat(markAsError(t("delete.modelDeleteFailed", name, error.localizedMessage)))
                return@withLock
            }
            chat(t("delete.modelDeleted", name))
        }
    }

    private suspend fun CmdI18n.reloadModel() {
        mutationMutex.withLock {
            ModelManager.reload()
        }
        chat(t("reload.modelsReloaded"))
    }

    private fun CmdI18n.browseModel(): Int {
        Util.getPlatform().openFile(modelsFolder)
        chat(regular("Location: "), clickablePath(modelsFolder))
        return 1
    }

    private suspend fun CmdI18n.trainModel(
        name: String,
        model: TwoDimensionalRegressionModel? = null
    ): Unit = try {
        val (samples, sampleTime) = measureTimedValue {
            CombatSample.parse(
                // Combat data
                DebugCombatRecorder.folder,
                // Trainer data
                DebugCombatTrainerRecorder.folder
            )
        }

        if (samples.isEmpty()) {
            chat(markAsError(t("create.noSamples")))
            return
        }

        chat(t("create.samplesLoaded", samples.size, sampleTime.toString(DurationUnit.SECONDS, decimals = 2)))

        class Dataset(val features: FloatArray, val labels: FloatArray)

        val (dataset, datasetTime) = measureTimedValue {
            val inputSize = samples.first().inputSize
            val outputSize = samples.first().outputSize
            require(inputSize > 0 && outputSize > 0) { "Sample input and output sizes must be positive" }
            val features = FloatArray(Math.multiplyExact(samples.size, inputSize))
            val labels = FloatArray(Math.multiplyExact(samples.size, outputSize))
            var featureIndex = 0
            var labelIndex = 0

            for (sample in samples) {
                require(sample.inputSize == inputSize && sample.outputSize == outputSize) {
                    "All samples must have the same input and output sizes"
                }
                val nextFeatureIndex = sample.fillAsInput(features, featureIndex)
                val nextLabelIndex = sample.fillAsOutput(labels, labelIndex)
                check(nextFeatureIndex == featureIndex + inputSize) { "Sample wrote an unexpected number of inputs" }
                check(nextLabelIndex == labelIndex + outputSize) { "Sample wrote an unexpected number of outputs" }
                featureIndex = nextFeatureIndex
                labelIndex = nextLabelIndex
            }

            Dataset(features, labels)
        }

        chat(t("create.preparedData", datasetTime.toString(DurationUnit.SECONDS, decimals = 2)))

        val trainingTime = measureTime {
            TwoDimensionalRegressionModel(name, models).use { candidate ->
                if (model != null) {
                    candidate.load(model.name)
                }

                candidate.train(dataset.features, dataset.labels)
                candidate.save()
            }

            ModelManager.reload()

            withContext(MinecraftDispatcher) {
                models.setByString(name)
                ModuleClickGui.sync()
            }
        }

        chat(t("create.trainingEnd", name, trainingTime.toString(DurationUnit.MINUTES, decimals = 2)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        chat(markAsError(t("create.trainingFailed", e.localizedMessage)))
    }

}
