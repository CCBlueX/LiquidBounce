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
package net.ccbluex.liquidbounce.utils.range

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import kotlin.math.abs
import kotlin.math.max

/**
 * Allows adjusting your attack range and scan range.
 */
open class RangeConfigurable : Configurable("Range"), MinecraftShortcuts {

    internal val maxAttackRange
        get() = player.entityAttackRange().effectiveMaxRange(player) + abs(maxRangeModifier)

    internal val minAttackRange
        get() = max(0f, player.entityAttackRange().effectiveMinRange(player) - abs(maxRangeModifier))

    internal val attackThroughWallsRange
        get() = wallRange

    /**
     * This will be added to the attack max-range.
     */
    private val maxRangeModifier by float("MaxRangeModifier", 0f, 0.0f..5f, "blocks")

    /**
     * This will be subtracted from the attack min-range.
     */
    private val minRangeModifier by float("MinRangeModifier", -0.0f, -2.0f..0f, "blocks")

    /**
     * This will use only this value for non-visible entities. Originally, we could never attack through walls,
     * so this makes sense to keep starting from 0.0
     */
    private val wallRange by float("WallRange", 3f, 0f..8f, "blocks")

}
