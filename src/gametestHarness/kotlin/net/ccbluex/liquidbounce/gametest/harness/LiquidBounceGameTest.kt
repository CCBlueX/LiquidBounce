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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.TestWorld
import net.ccbluex.gametest.harness.verdict.ClientErrors
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.screens.TitleScreen

/**
 * A game test of LiquidBounce or an add-on on the game test server. It runs with no module but the HUD, as AutoConfig
 * would load a config on join, and turns the modules that ran before back on afterwards.
 */
abstract class LiquidBounceGameTest(world: TestWorld, body: GameTestScope.() -> Unit) : ServerGameTest(world, body) {

    constructor(body: GameTestScope.() -> Unit) : this(TestWorld.FLAT, body)

    private var running = emptyList<ClientModule>()

    override fun awaitClient(context: ClientGameTestContext) {
        // LiquidBounce holds a progress screen until its startup tasks are done, and aborting the browser's
        // first page load by joining a server is fatal to it
        context.waitFor({ it.gui.screen() is TitleScreen }, STARTUP_TIMEOUT)
        context.waitFor({ LiquidBounce.isInitialized && ScreenManager.mainBrowser != null }, STARTUP_TIMEOUT)
    }

    override fun beforeJoin(context: ClientGameTestContext) = context.runOnClient<RuntimeException> {
        running = runningModules()
        running.forEach { it.enabled = false }
    }

    override fun afterTest(context: ClientGameTestContext) = context.runOnClient<RuntimeException> {
        runningModules().forEach { it.enabled = false }
        running.forEach { it.enabled = true }
    }

    private companion object {
        const val STARTUP_TIMEOUT = 20 * 60 * 5

        init {
            // What a test machine without a LiquidBounce account or a GPU logs anyway
            ClientErrors.ignore(
                "CoroutineScope of ModuleClickGUI", "LiquidChat", "Unable to receive update information",
                "[LiquidBounce/Chromium]", "The render process of", "https://api.liquidbounce.net/",
            )
        }

        fun runningModules() = ModuleManager.filter { it.enabled && it !== ModuleHud }
    }

}
