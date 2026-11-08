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
package net.ccbluex.liquidbounce.gametest.combat

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoPearl
import net.ccbluex.liquidbounce.gametest.onClient

/**
 * AutoPearl: the throw delay holds the pearl back by that many ticks.
 */
fun CombatScene.checkAutoPearlThrowDelay() {
    startAutoPearl {}

    val delayed = pearlThrownWithDelay("8..8")
    val immediate = pearlThrownWithDelay("0..0")
    check(delayed - immediate >= 6) {
        "AutoPearl: thrown after $delayed ticks with an 8 tick delay and after $immediate ticks without"
    }

    context.onClient { ModuleAutoPearl.enabled = false }
}

private fun CombatScene.pearlThrownWithDelay(delay: String): Int {
    context.onClient { ModuleAutoPearl.set("ThrowDelay", delay) }
    // Let the pearls arrive in the hotbar and the cooldown of the previous throw run out
    context.waitTicks(PEARL_TICKS)

    return checkNotNull(pearlThrownAfter(null, at(0.0, 1.5, 3.0))) {
        "AutoPearl: no pearl thrown with a $delay tick delay"
    }
}
