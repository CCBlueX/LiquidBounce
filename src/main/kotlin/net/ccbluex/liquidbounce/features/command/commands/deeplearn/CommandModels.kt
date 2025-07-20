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
 *
 *
 */
package net.ccbluex.liquidbounce.features.command.commands.deeplearn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.ModelHolster
import net.ccbluex.liquidbounce.deeplearn.ModelHolster.models
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.deeplearn.models.MinaraiModel
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiCombatRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiTrainer
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.minecraft.util.Util
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

object CommandModels : CommandFactory {

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
            .suspendHandler { command, args ->
                val name = args[0] as String

                // Check if model exists
                if (models.choices.any { model -> model.name.equals(name, true) }) {
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
            .suspendHandler { command, args ->
                val name = args[0] as String
                val model = models.choices.find { model -> model.name.equals(name, true) } ?:
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
            .handler { command, args ->
                val name = args[0] as String
                val model = models.choices.find { model -> model.name.equals(name, true) }

                if (model == null) {
                    chat(markAsError(command.result("modelNotFound", name)))
                    return@handler
                }

                model.delete()
                models.choices.remove(model)
                chat(command.result("modelDeleted", name))
            }
            .build()
    }


    private fun reloadModelCommand(): Command {
        return CommandBuilder
            .begin("reload")
            .handler { command, _ ->
                ModelHolster.reload()
                chat(command.result("modelsReloaded"))
            }
            .build()
    }

    private fun browseModelCommand(): Command {
        return CommandBuilder
            .begin("browse")
            .handler { command, _ ->
                Util.getOperatingSystem().open(modelsFolder)
                chat(regular("Location: "), clickablePath(modelsFolder))
            }
            .build()
    }

    private fun trainModel(command: Command, name: String, model: MinaraiModel? = null) = runCatching {
        val (samples, sampleTime) = measureTimedValue {
            TrainingData.parse(
                // Combat data
                MinaraiCombatRecorder.folder,
                // Trainer data
                MinaraiTrainer.folder
            )
        }

        if (samples.isEmpty()) {
            chat(markAsError(command.result("noSamples")))
            return@runCatching
        }

        chat(command.result("samplesLoaded", samples.size, sampleTime.toString(DurationUnit.SECONDS, decimals = 2)))

        @Suppress("ArrayInDataClass")
        data class Dataset(val features: Array<FloatArray>, val labels: Array<FloatArray>)

        val (dataset, datasetTime) = measureTimedValue {
            Dataset(
                samples.mapArray(TrainingData::asInput),
                samples.mapArray(TrainingData::asOutput)
            )
        }

        chat(command.result("preparedData", datasetTime.toString(DurationUnit.SECONDS, decimals = 2)))

        val trainingTime = measureTime {
            val model = model ?: MinaraiModel(name, models).also { model -> models.choices.add(model) }
            model.train(dataset.features, dataset.labels)
            model.save()

            models.setByString(model.name)
            ModuleClickGui.reload()
        }

        chat(command.result("trainingEnd", name, trainingTime.toString(DurationUnit.MINUTES, decimals = 2)))
    }.onFailure { error ->
        chat(markAsError(command.result("trainingFailed", error.localizedMessage)))
    }

}
