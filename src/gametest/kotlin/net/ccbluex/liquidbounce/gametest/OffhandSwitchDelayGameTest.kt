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

import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The Offhand Totem/SwitchDelay range: every switch waits at least its lower end, and the wait differs between
 * switches.
 */
class OffhandSwitchDelayGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        configure("Totem/SwitchDelay" to "$MIN_DELAY_MS..$MAX_DELAY_MS")
        context.onClient { ModuleOffhand.enabled = true }
        giveTotems(REFILLS + 1)

        // the first refill only starts the measurement
        refill()
        val delays = List(REFILLS) { refill() }

        check(delays.min() >= MIN_DELAY_MS - LATENCY_MS) {
            "Refilled ${delays.min()}ms after the last switch, the shortest delay is $MIN_DELAY_MS"
        }
        check(delays.max() - delays.min() >= SPREAD_MS) {
            "The delays $delays of the range $MIN_DELAY_MS..$MAX_DELAY_MS differ by less than $SPREAD_MS"
        }
    }

    private companion object {
        const val MIN_DELAY_MS = 500L
        const val MAX_DELAY_MS = 1700L
        const val LATENCY_MS = 250L

        /**
         * Eight delays out of a range of 1200 ms lie this close together once in 40000 runs.
         */
        const val SPREAD_MS = 200L
        const val REFILLS = 8
    }

}
