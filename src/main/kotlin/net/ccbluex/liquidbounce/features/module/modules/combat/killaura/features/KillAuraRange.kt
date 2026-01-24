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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.math.max

/**
 * Allows adjusting your attack range and scan range.
 */
object KillAuraRange : Configurable("Range"), MinecraftShortcuts {

    internal val maxAttackRange
        get() = player.entityAttackRange().effectiveMaxRange(player) + adjustRange.endInclusive

    internal val minAttackRange
        get() = max(0f, player.entityAttackRange().effectiveMinRange(player) + adjustRange.start)

    internal val attackThroughWallsRange
        get() = wallRange

    internal val scanRange
        get() = maxOf(maxAttackRange, wallRange) + currentScanExtraRange

    /**
     * This will be added to the normal entity interaction range.
     */
    private val adjustRange by floatRange("AdjustRange", -2.0f..1.0f, -1.0f..8f)

    /**
     * This will use only this value for non-visible entities. Originally, we could never attack through walls,
     * so this makes sense to keep starting from 0.0
     */
    private val wallRange by float("WallRange", 3f, 0f..8f)

    private val scanExtraRange by floatRange("AddScanRange", 2.0f..3.0f, 0.0f..7.0f).onChanged { range ->
        currentScanExtraRange = range.random()
    }
    private var currentScanExtraRange: Float = scanExtraRange.random()

    fun update() {
        currentScanExtraRange = scanExtraRange.random()
    }

}
