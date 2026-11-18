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

package net.ccbluex.liquidbounce.features.global.viewmodel

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwingAnimationPriorityTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    private object NonSwingMode : Mode("None") {
        override val parent: ModeValueGroup<*>
            get() = error("The priority decision does not access the mode parent")
    }

    @Test
    fun `selected swing animation takes priority over sword blocking`() {
        for (mode in listOf(SwingAnimationMode.Hook, SwingAnimationMode.Swipe)) {
            assertFalse(
                SwingAnimationPriority.shouldUseBlockingVisual(
                    viewModelRunning = true,
                    blockingApplies = true,
                    activeMode = mode
                )
            )
        }
    }

    @Test
    fun `sword blocking is preserved without a selected swing animation`() {
        for (mode in listOf(null, NonSwingMode)) {
            assertTrue(
                SwingAnimationPriority.shouldUseBlockingVisual(
                    viewModelRunning = true,
                    blockingApplies = true,
                    activeMode = mode
                )
            )
        }
    }

    @Test
    fun `disabled view model preserves sword blocking`() {
        assertTrue(
            SwingAnimationPriority.shouldUseBlockingVisual(
                viewModelRunning = false,
                blockingApplies = true,
                activeMode = SwingAnimationMode.Hook
            )
        )
    }

    @Test
    fun `blocking visual stays disabled when sword blocking does not apply`() {
        for (mode in listOf(null, NonSwingMode, SwingAnimationMode.Hook)) {
            assertFalse(
                SwingAnimationPriority.shouldUseBlockingVisual(
                    viewModelRunning = true,
                    blockingApplies = false,
                    activeMode = mode
                )
            )
        }
    }
}
