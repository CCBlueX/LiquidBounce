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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.ModelManager
import net.ccbluex.liquidbounce.deeplearn.ModelManager.models
import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModel
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatTrainerRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.util.Util
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

object CommandModels : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("models")
            .hub()
            .subcommand(createModelCommand())
            .subcommand(improveModelCommand())
            .subcommand(deleteModelCommand())
            .subcommand(reloadModelCommand())
            .subcommand(browseModelCommand())
            .build()
    }

    private fun createModelCommand(): Command {
        return CommandBuilder
            .begin("create")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .required()
                    .build()
            )
            .suspendHandler {
                val name = args[0] as String

                // Check if model exists
                if (models.modes.any { model -> model.name.equals(name, true) }) {
                    throw CommandException(command.result("modelExists", name))
                }

                // Check if the name is a valid name
                if (name.contains(Regex("[^a-zA-Z0-9-]"))) {
                    throw CommandException(command.result("invalidName"))
                }

                chat(command.result("trainingStart", name))
                withContext(Dispatchers.Default) {
                    trainModel(command, name)
                }
            }
            .build()
    }

    private fun improveModelCommand(): Command {
        return CommandBuilder
            .begin("improve")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .required()
                    .build()
            )
            .suspendHandler {
                val name = args[0] as String
                val model = models.modes.find { model -> model.name.equals(name, true) } ?:
                    throw CommandException(command.result("modelNotFound", name))

                chat(command.result("trainingStart", name))
                withContext(Dispatchers.Default) {
                    trainModel(command, name, model)
                }
            }
            .build()
    }

    private fun deleteModelCommand(): Command {
        return CommandBuilder
            .begin("delete")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .required()
                    .build()
            )
            .handler {
                val name = args[0] as String
                val model = models.modes.find { model -> model.name.equals(name, true) }

                if (model == null) {
                    chat(markAsError(command.result("modelNotFound", name)))
                    return@handler
                }

                model.delete()
                models.modes.remove(model)
                chat(command.result("modelDeleted", name))
            }
            .build()
    }


    private fun reloadModelCommand(): Command {
        return CommandBuilder
            .begin("reload")
            .handler {
                ModelManager.reload()
                chat(command.result("modelsReloaded"))
            }
            .build()
    }

    private fun browseModelCommand(): Command {
        return CommandBuilder
            .begin("browse")
            .handler {
                Util.getPlatform().openFile(modelsFolder)
                chat(regular("Location: "), clickablePath(modelsFolder))
            }
            .build()
    }

    private fun trainModel(command: Command, name: String, model: TwoDimensionalRegressionModel? = null) = runCatching {
        val (samples, sampleTime) = measureTimedValue {
            CombatSample.parse(
                // Combat data
                DebugCombatRecorder.folder,
                // Trainer data
                DebugCombatTrainerRecorder.folder
            )
        }

        if (samples.isEmpty()) {
            chat(markAsError(command.result("noSamples")))
            return@runCatching
        }

        chat(command.result("samplesLoaded", samples.size, sampleTime.toString(DurationUnit.SECONDS, decimals = 2)))

        class Dataset(val features: Array<FloatArray>, val labels: Array<FloatArray>)

        val (dataset, datasetTime) = measureTimedValue {
            Dataset(
                samples.mapToArray { it.asInput },
                samples.mapToArray { it.asOutput }
            )
        }

        chat(command.result("preparedData", datasetTime.toString(DurationUnit.SECONDS, decimals = 2)))

        val trainingTime = measureTime {
            val model = model ?: TwoDimensionalRegressionModel(name, models).also { model -> models.modes.add(model) }
            model.train(dataset.features, dataset.labels)
            model.save()

            models.setByString(model.name)
            ModuleClickGui.sync()
        }

        chat(command.result("trainingEnd", name, trainingTime.toString(DurationUnit.MINUTES, decimals = 2)))
    }.onFailure { error ->
        chat(markAsError(command.result("trainingFailed", error.localizedMessage)))
    }

}
