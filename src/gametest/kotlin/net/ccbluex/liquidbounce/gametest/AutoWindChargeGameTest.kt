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

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWindCharge
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.item.Items

/**
 * Holding jump with [ModuleAutoWindCharge] has to launch the player high on every bounce, with one charge each.
 *
 * The flat world of the test API has solid ground everywhere, so each charge bursts right below the player.
 */
class AutoWindChargeGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode survival @a")
            world.server.runCommand("give @a minecraft:wind_charge $CHARGES")
            context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)

            val groundY = context.fromClient { client -> checkNotNull(client.player).y }
            context.onClient { ModuleAutoWindCharge.enabled = true }
            context.input.holdKey { options -> options.keyJump }

            val peaks = bouncePeaks(context, groundY)

            context.input.releaseKey { options -> options.keyJump }
            context.onClient { ModuleAutoWindCharge.enabled = false }

            val thrown = CHARGES - context.fromClient { client ->
                checkNotNull(client.player).inventory.countItem(Items.WIND_CHARGE)
            }

            check(peaks.size == BOUNCES && peaks.all { it >= MIN_PEAK }) { "bounces too low: $peaks" }
            check(thrown == BOUNCES) { "$thrown charges thrown for $BOUNCES bounces" }
        }
    }

    /**
     * Heights above the ground the player reached before each landing.
     */
    private fun bouncePeaks(context: ClientGameTestContext, groundY: Double): List<Double> {
        val peaks = mutableListOf<Double>()
        var peak = 0.0
        var ticks = 0

        while (peaks.size < BOUNCES && ticks++ < BOUNCE_TIMEOUT_TICKS) {
            context.waitTick()
            val (height, onGround) = context.fromClient { client ->
                val player = checkNotNull(client.player)
                player.y - groundY to player.onGround()
            }

            if (!onGround) {
                peak = maxOf(peak, height)
            } else if (peak > 0.0) {
                peaks += peak
                peak = 0.0
            }
        }

        return peaks
    }

    private companion object {
        const val CHARGES = 16
        const val BOUNCES = 3

        // A jump reaches 1.25 blocks, a charge thrown while standing still about 6
        const val MIN_PEAK = 5.0

        const val SETTLE_TIMEOUT_TICKS = 200
        const val BOUNCE_TIMEOUT_TICKS = 300
    }

}
