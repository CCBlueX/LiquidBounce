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
package net.ccbluex.liquidbounce.render.splash

import net.ccbluex.liquidbounce.LiquidBounce

/**
 * The bar's progress over the whole start. Minecraft's resources and the client's startup tasks share the bar
 * from the beginning, so it never runs back when the tasks take over after loading.
 */
object SplashProgress {

    /** The resources' share of the bar while the client starts. */
    private const val RESOURCES = 0.6f

    /** True while the splash belongs to the client's start, set when a splash begins. */
    @Volatile
    internal var startup = true

    /**
     * The bar while Minecraft loads [resources] (0 to 1).
     */
    @JvmStatic
    fun loading(resources: Float) = if (startup) resources * RESOURCES + tasks() * (1f - RESOURCES) else resources

    /**
     * The bar once the resources are loaded, while the startup tasks run at [tasks] (0 to 1).
     */
    fun afterLoading(tasks: Float) = RESOURCES + tasks.coerceIn(0f, 1f) * (1f - RESOURCES)

    private fun tasks(): Float {
        val tasks = LiquidBounce.taskManager ?: return 0f
        return if (tasks.isCompleted) 1f else tasks.progress
    }

}
