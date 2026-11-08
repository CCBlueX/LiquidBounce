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
package net.ccbluex.liquidbounce.gametest.visual

import net.ccbluex.liquidbounce.features.module.modules.render.customambience.ModuleCustomAmbience
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The free time of day of [ModuleCustomAmbience].
 */
class CustomTimeGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld { world ->
            world.server.runCommand("time set 1000")
            context.onClient { ModuleCustomAmbience.enabled = true }
            context.setValue("customAmbience.time", "Custom")
            context.setValue("customAmbience.customTime", CUSTOM_TIME.toString())
            context.waitTick()

            val clock = context.fromClient { client -> client.level().overworldClockTime }
            check(clock == CUSTOM_TIME) { "the custom time shows $clock instead of $CUSTOM_TIME" }
            context.takeScreenshot("visual_custom_time")

            context.onClient { ModuleCustomAmbience.enabled = false }
        }
    }

    private companion object {
        const val CUSTOM_TIME = 18000L
    }

}
