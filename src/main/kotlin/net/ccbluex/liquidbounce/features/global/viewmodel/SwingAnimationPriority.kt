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

/**
 * Resolves which visual wins when a sword-blocking pose and a swing animation are both applicable.
 * A selected [SwingAnimationMode] was chosen explicitly, so the blocking visual must not replace it.
 * With the view model disabled, the pre-existing blocking behaviour is preserved.
 */
object SwingAnimationPriority {

    @JvmStatic
    fun shouldUseBlockingVisual(running: Boolean, blockingApplies: Boolean, activeMode: Mode?): Boolean =
        blockingApplies && (!running || activeMode !is SwingAnimationMode)
}
