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
import net.ccbluex.liquidbounce.config.types.group.NoneMode
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

    private fun noneMode(): NoneMode {
        val created = mutableListOf<NoneMode>()
        ModeValueGroup<Mode>(null, "SwingAnimationForTest", { 0 }) { modes ->
            arrayOf<Mode>(NoneMode(modes).also { created.add(it) })
        }
        return created.single()
    }

    @Test
    fun `does not use blocking visual when Hook is selected and blocking applies`() {
        assertFalse(
            SwingAnimationPriority.shouldUseBlockingVisual(true, SwingAnimationMode.Hook),
            "Hook was selected explicitly, so the blocking pose must not take over"
        )
    }

    @Test
    fun `does not use blocking visual when a different swing mode is selected`() {
        assertFalse(
            SwingAnimationPriority.shouldUseBlockingVisual(true, SwingAnimationMode.Swipe),
            "Any real swing mode, not just Hook, must keep priority over the blocking pose"
        )
    }

    @Test
    fun `uses blocking visual when None mode is selected and blocking applies`() {
        assertTrue(
            SwingAnimationPriority.shouldUseBlockingVisual(true, noneMode()),
            "None keeps the vanilla blocking behaviour unchanged"
        )
    }

    @Test
    fun `uses blocking visual when active mode is null and blocking applies`() {
        assertTrue(
            SwingAnimationPriority.shouldUseBlockingVisual(true, null),
            "A missing active mode must not disable the blocking visual"
        )
    }

    @Test
    fun `does not use blocking visual when blocking does not apply`() {
        assertFalse(
            SwingAnimationPriority.shouldUseBlockingVisual(false, SwingAnimationMode.Hook),
            "Without a blocking condition no blocking visual can be forced"
        )
        assertFalse(
            SwingAnimationPriority.shouldUseBlockingVisual(false, noneMode()),
            "Without a blocking condition the blocking visual stays inactive for None too"
        )
    }

    @Test
    fun `returns the same result when called twice with identical arguments`() {
        val first = SwingAnimationPriority.shouldUseBlockingVisual(true, SwingAnimationMode.Hook)
        val second = SwingAnimationPriority.shouldUseBlockingVisual(true, SwingAnimationMode.Hook)

        assertTrue(first == second, "The decision must be free of cumulative effects")
    }
}
