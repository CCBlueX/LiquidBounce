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
import net.ccbluex.liquidbounce.integration.backend.isBrowserDisabled
import net.ccbluex.liquidbounce.integration.task.type.ResourceTask
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.splash.SplashExitOverlay
import net.ccbluex.liquidbounce.render.splash.SplashPhaseScreen
import net.ccbluex.liquidbounce.render.splash.SplashProgress
import net.ccbluex.liquidbounce.render.splash.SplashTimeline
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.formatAsCapacity
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import java.text.DecimalFormat

/**
 * Splash phase while the startup tasks run, with their status below the progress bar.
 */
class TaskProgressScreen(
    title: Component,
    private val taskManager: TaskManager
) : SplashPhaseScreen(title, SplashTimeline.Phase.TASKS) {

    private val percentFormat = DecimalFormat("0")

    private var completedAt = -1L

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val now = Util.getMillis()
        val progress = if (completedAt == -1L) taskManager.progress else 1f
        extractSplash(graphics, SplashProgress.afterLoading(progress))

        val fadeIn = ((age - 150f) / 400f).coerceIn(0f, 1f)
        val fadeOut = if (completedAt == -1L) 1f else 1f - ((now - completedAt) / TEXT_FADE_OUT).coerceIn(0f, 1f)
        val alpha = fadeIn * fadeOut

        var y = contentTop
        for ((index, line) in statusLines(progress).withIndex()) {
            val main = index == 0
            extractCenteredText(graphics, line, y, alpha * if (main) 0.8f else 0.45f, if (main) 1f else 0.85f)
            y += font.lineHeight * if (main) 1.4f else 1.1f
        }
    }

    private fun statusLines(progress: Float) = buildList {
        val activeTasks = taskManager.getActiveTasks()
        add("${title.string} · ${percentFormat.format(progress * 100)}%${formatTotalSpeed(activeTasks)}")

        activeTasks.take(3).forEach { task ->
            add("${task.name} · ${percentFormat.format(task.progress * 100)}%${formatTotalSpeed(listOf(task))}")
        }

        if (activeTasks.size > 3) {
            add(translation("liquidbounce.splash.moreTasks", activeTasks.size - 3).string)
        }
    }

    private fun formatTotalSpeed(tasks: List<Task>): String {
        val total = calculateTotalSpeed(tasks)

        return if (total > 0) {
            " · ${total.formatAsCapacity()}/s"
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
        if (completedAt == -1L) {
            val browserReady = BrowserBackendManager.backend?.isInitialized == true || isBrowserDisabled
            if (taskManager.isCompleted && browserReady) {
                completedAt = Util.getMillis()
            }
        } else if (Util.getMillis() - completedAt >= TEXT_FADE_OUT + 100f) {
            mc.gui.setScreen(TitleScreen())
            mc.gui.setOverlay(SplashExitOverlay())
        }
    }

    private companion object {
        const val TEXT_FADE_OUT = 300f
    }

}
