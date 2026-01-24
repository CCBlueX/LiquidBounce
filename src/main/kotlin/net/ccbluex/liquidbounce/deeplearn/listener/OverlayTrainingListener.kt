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
package net.ccbluex.liquidbounce.deeplearn.listener

import ai.djl.training.Trainer
import ai.djl.training.listener.TrainingListener
import ai.djl.training.listener.TrainingListenerAdapter
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.textOf
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting

/**
 * Displays training overlay in Minecraft
 *
 * Training Epoch 1/10 - Batch 45%
 * [███████████░░░░░░░░░░░░░░]
 */
class OverlayTrainingListener(
    private val maxEpoch: Int
) : TrainingListenerAdapter() {

    private var numEpochs = 0

    override fun onEpoch(trainer: Trainer?) {
        numEpochs++
        super.onEpoch(trainer)
    }

    override fun onTrainingBatch(trainer: Trainer, batchData: TrainingListener.BatchData) {
        reportBatchData(batchData)
        super.onTrainingBatch(trainer, batchData)
    }

    override fun onValidationBatch(trainer: Trainer, batchData: TrainingListener.BatchData) {
        reportBatchData(batchData)
        super.onValidationBatch(trainer, batchData)
    }

    fun reportBatchData(batchData: TrainingListener.BatchData) {
        val batch = batchData.batch
        val progressCurrent = batch.progress
        val progressTotal = batch.progressTotal
        val progress = (progressCurrent.toFloat() / progressTotal.toFloat() * 100).toInt()

        val progressBar = textOf(
            regular("Training Epoch "),
            variable("$numEpochs/$maxEpoch"),
            regular(" - "),
            regular("Batch "),
            variable("$progress%"),
            regular("\n".repeat(1)),
            "[".asPlainText(ChatFormatting.GRAY),
            "█".repeat(progress / 4).asPlainText(ChatFormatting.GREEN),
            "░".repeat(25 - progress / 4).asPlainText(ChatFormatting.DARK_GRAY),
            "]".asPlainText(ChatFormatting.GRAY),
        )

        mc.execute {
            mc.gui.setOverlayMessage(progressBar, false)
        }
    }


}
