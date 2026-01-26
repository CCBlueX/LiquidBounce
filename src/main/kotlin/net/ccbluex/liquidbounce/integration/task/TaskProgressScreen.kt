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

package net.ccbluex.liquidbounce.integration.task

import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.task.type.ResourceTask
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.formatAsCapacity
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import java.text.DecimalFormat

/**
 * Screen that displays TaskManager progress
 */
class TaskProgressScreen(
    title: String,
    private val taskManager: TaskManager
) : Screen(title.asPlainText()) {

    private val percentFormat = DecimalFormat("0.0")

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderMenuBackground(context)

        val cx = width / 2.0
        val cy = height / 2.0

        val progressBarWidth = width / 1.5

        val poseStack = context.pose()

        // Progress
        val progress = taskManager.progress
        val textLines = getTaskLines(progress)

        // Draw text
        val textHeight = textLines.size * (font.lineHeight + 2)
        var yOffset = (cy - textHeight / 2).toInt() - 40

        // Draw title
        context.drawString(
            font,
            title.string.asPlainText(ChatFormatting.GOLD),
            (cx - font.width(title.string) / 2).toInt(),
            yOffset,
            -1,
            true
        )

        yOffset += font.lineHeight + 10

        // Draw task information
        for (line in textLines) {
            context.drawString(
                font,
                line,
                (cx - font.width(line) / 2).toInt(),
                yOffset,
                -1,
                false
            )
            yOffset += font.lineHeight + 2
        }

        val progressBarHeight = 14

        // Draw progress bar
        poseStack.pushMatrix()
        poseStack.translate(cx.toFloat(), yOffset.toFloat() + 18.0f)
        poseStack.translate(progressBarWidth.toFloat() * -0.5f, progressBarHeight.toFloat() * -0.5f)

        // Bar border
        context.fill(
            0, 0,
            progressBarWidth.toInt(), progressBarHeight.toInt(),
            -1
        )

        // Bar background
        context.fill(
            2, 2,
            (progressBarWidth - 2).toInt(), (progressBarHeight - 2).toInt(),
            ARGB.color(255, 24, 26, 27)
        )

        context.fill(
            4, 4,
            ((progressBarWidth - 4) * progress).toInt(), (progressBarHeight - 4).toInt(),
            -1
        )
        poseStack.popMatrix()
    }

    private fun getTaskLines(progress: Float): List<Component> {
        val activeTasks = taskManager.getActiveTasks()
        val speed = formatTotalSpeed(activeTasks)

        // Prepare text to display
        val textLines = mutableListOf<Component>()
        textLines.add("Total: ${percentFormat.format(progress * 100)}%$speed".asPlainText())
        textLines.add(PlainText.EMPTY)

        activeTasks.take(3).forEach { task ->
            textLines.add(Pools.buildStringPooled {
                append(task.name)
                append(": ")
                append(percentFormat.format(task.progress * 100))
                append("%")
                append(formatTotalSpeed(listOf(task)))
            }.asPlainText(ChatFormatting.GRAY))
        }

        if (activeTasks.size > 3) {
            textLines.add("... and ${activeTasks.size - 3} more tasks".asPlainText(ChatFormatting.GRAY))
        }
        return textLines
    }

    private fun formatTotalSpeed(tasks: List<Task>): String {
        val total = calculateTotalSpeed(tasks)

        return if (total > 0) {
            " (${total.formatAsCapacity()}/s)"
        } else {
            ""
        }
    }

    private fun calculateTotalSpeed(tasks: List<Task>): Long {
        return tasks.filter { task ->
            !task.isCompleted
        }.sumOf { task ->
            ((task as? ResourceTask)?.speed ?: 0L) + calculateTotalSpeed(task.subTasks.values.toList())
        }
    }

    override fun tick() {
        if (taskManager.isCompleted && (BrowserBackendManager.backend?.isInitialized == true ||
                BrowserBackendManager.isSkipping)) {
            mc.setScreen(TitleScreen())
        }
    }

    override fun shouldCloseOnEsc() = false

    override fun isPauseScreen() = false

}
