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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup

object KillAuraRotationsValueGroup : RotationsValueGroup(ModuleKillAura, combatSpecific = true) {

    val rotationTiming by enumChoice("RotationTiming", KillAuraRotationTiming.NORMAL)
    val aimThroughWalls by boolean("ThroughWalls", false)

    /**
     * When enabled, if current rotation can still raytrace the target, skip rotating.
     */
    val lazyRotation by boolean("LazyRotation", false)

    enum class KillAuraRotationTiming(override val tag: String) : Tagged {
        NORMAL("Normal"),
        SNAP("Snap"),
        ON_TICK("OnTick"),
        DYNAMIC("Dynamic")
    }

    /**
     * Speed curve is `base / ticksUntilAttack`, so it rises steeply only in the last couple of ticks
     * (e.g. 50% at 2 ticks out, 100% at 1 tick and on the click tick) rather than linearly.
     */
    fun dynamicSpeedDivisor(ticksUntilAttack: Int): Float {
        return ticksUntilAttack.coerceAtLeast(1).toFloat()
    }

}
