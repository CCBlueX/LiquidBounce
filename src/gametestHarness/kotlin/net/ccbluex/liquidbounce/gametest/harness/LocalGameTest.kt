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
package net.ccbluex.liquidbounce.gametest.harness

import net.ccbluex.gametest.harness.GameTestResults
import net.ccbluex.gametest.harness.connection.GameTestEnvironment
import net.ccbluex.liquidbounce.LiquidBounce
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.screens.TitleScreen

/**
 * A game test of the client alone that needs no world, such as commands, configs and registries; a test that does
 * plays on the integrated server as a [LiquidBounceGameTest] with `integrated` set. It runs only when no anticheat is
 * picked; `-PgameTestServerOnly` selects these too.
 */
abstract class LocalGameTest : FabricClientGameTest {

    private val name = javaClass.simpleName.removeSuffix("GameTest")

    final override fun runTest(context: ClientGameTestContext) {
        if (!localRun || !GameTestEnvironment.isSelected(name)) {
            return
        }
        // LiquidBounce holds a progress screen until its startup tasks, such as the DJL natives, are done
        context.waitFor({ it.gui.screen() is TitleScreen }, STARTUP_TIMEOUT)
        check(context.computeOnClient<Boolean, RuntimeException> { LiquidBounce.isInitialized }) {
            "LiquidBounce did not finish initializing"
        }
        GameTestResults.local(name) { test(context) }
    }

    protected abstract fun test(context: ClientGameTestContext)

    private companion object {
        const val STARTUP_TIMEOUT = 20 * 60 * 5

        /** CI runs the tests of the client alone in a job of their own, without an anticheat. */
        val localRun = System.getenv("GAMETEST_SERVER_ANTICHEAT").isNullOrBlank()
    }

}
