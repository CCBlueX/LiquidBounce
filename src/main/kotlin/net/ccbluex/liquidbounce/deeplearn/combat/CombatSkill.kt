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
package net.ccbluex.liquidbounce.deeplearn.combat

import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi

/**
 * The model is told how well each fighter trades hits, so it learns what stronger players do differently
 * from all fights, and plays as one by being given [TARGET].
 */
@UnstableAddonApi
object CombatSkill {
    /** Hit share of the best quarter of observed players. */
    const val TARGET = 0.62f

    fun input(timeline: CombatTimeline) = ((timeline.skill - 0.5f) * 4f).coerceIn(-2f, 2f)
}
