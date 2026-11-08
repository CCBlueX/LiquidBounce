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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.LONG_TIMEOUT
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The CrystalAura Place/Delay and Destroy/Delay ranges.
 */
class CrystalAuraDelayGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inCrystalAuraArena {
        checkIntervals("Place/Delay", placed = true)
        checkIntervals("Destroy/Delay", placed = false)
    }

    /**
     * Sets [option] to the delay range and checks the time between crystals appearing on [CrystalAuraArena.a]
     * when [placed], or else being destroyed there. The ids tell the crystals apart, as a destroyed crystal can be
     * replaced within the same tick.
     */
    private fun CrystalAuraArena.checkIntervals(option: String, placed: Boolean) {
        configure(option to "$MIN_DELAY_MS..$MAX_DELAY_MS")
        context.onClient { ModuleCrystalAura.enabled = true }

        val times = mutableListOf<Long>()
        var ids = emptyList<Int>()
        context.ticksUntil(LONG_TIMEOUT) {
            val now = crystalIdsOn(a)
            val changed = if (placed) now.any { it !in ids } else ids.any { it !in now }
            if (changed) {
                times += System.currentTimeMillis()
            }
            ids = now
            times.size == DELAY_SAMPLES
        }

        context.onClient { ModuleCrystalAura.enabled = false }
        configure(option to "0..0")
        clearCrystals()

        val intervals = times.zipWithNext { first, second -> second - first }
        check(intervals.size == DELAY_SAMPLES - 1) { "Only ${times.size} changes with $option" }
        // one tick of slack for when the change was observed
        check(intervals.all { it >= MIN_DELAY_MS - TICK_MS }) { "$option was not kept: $intervals" }
    }

    private companion object {
        const val MIN_DELAY_MS = 400
        const val MAX_DELAY_MS = 700
        const val TICK_MS = 50
        const val DELAY_SAMPLES = 4
    }

}
