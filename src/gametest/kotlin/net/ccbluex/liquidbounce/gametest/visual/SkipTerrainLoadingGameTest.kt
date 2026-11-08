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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.gui.screens.LevelLoadingScreen
import net.minecraft.world.level.Level

/**
 * The skipped terrain loading screen of [ModuleAntiBlind] when changing the dimension.
 */
class SkipTerrainLoadingGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld { world ->
            context.onClient { ModuleAntiBlind.enabled = true }
            val home = context.fromClient { client -> client.player().blockPosition() }

            context.setValue("antiBlind.skipTerrainLoading", "true")
            val skipped = changeDimension(context, world, Level.NETHER.identifier().toString(), "0 $NETHER_Y 0")
            context.takeScreenshot("visual_antiblind_skipped_loading")

            context.setValue("antiBlind.skipTerrainLoading", "false")
            val vanilla = changeDimension(
                context, world, Level.OVERWORLD.identifier().toString(), "${home.x} ${home.y} ${home.z}"
            )

            println("Terrain loading screen: $skipped ticks skipped, $vanilla ticks vanilla")
            check(skipped <= MAX_SKIPPED_LOADING_TICKS && skipped <= vanilla) {
                "the loading screen stayed $skipped ticks, $vanilla without skipping"
            }
            context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)

            context.onClient { ModuleAntiBlind.enabled = false }
        }
    }

    /**
     * Ticks the loading screen stays open when moving to [position] in [dimension].
     */
    private fun changeDimension(
        context: ClientGameTestContext,
        world: TestSingleplayerContext,
        dimension: String,
        position: String,
    ): Int {
        world.server.runCommand("execute in $dimension run tp @a $position")

        var loadingTicks = 0
        context.waitFor({ client ->
            val loading = client.gui.screen() is LevelLoadingScreen
            if (loading) {
                loadingTicks++
            }
            !loading && client.level?.dimension()?.identifier()?.toString() == dimension
        }, SETTLE_TIMEOUT_TICKS)
        return loadingTicks
    }

    private companion object {
        const val NETHER_Y = 120
        const val MAX_SKIPPED_LOADING_TICKS = 5
    }

}
