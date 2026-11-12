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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.give
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWindCharge
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.minecraft.world.item.Items

private const val CHARGES = 16
private const val BOUNCES = 3

// A jump reaches 1.25 blocks, a charge thrown while standing still about 6
private const val MIN_PEAK = 5.0

private const val BOUNCE_TIMEOUT_TICKS = 300

/**
 * Holding jump with [ModuleAutoWindCharge] has to launch the player high on every bounce, with one charge each.
 * The superflat ground is solid everywhere, so each charge bursts right below the player.
 */
class AutoWindChargeGameTest : LiquidBounceGameTest({
    give("minecraft:wind_charge", CHARGES)
    awaitClient("the wind charges") { checkNotNull(it.player).inventory.countItem(Items.WIND_CHARGE) == CHARGES }

    val groundY = client { checkNotNull(it.player).y }
    enable(ModuleAutoWindCharge)
    input.holdKey { it.keyJump }
    val peaks = bouncePeaks(groundY)
    input.releaseKey { it.keyJump }
    disable(ModuleAutoWindCharge)
    awaitClient("the landing") { checkNotNull(it.player).onGround() }

    check(peaks.size == BOUNCES && peaks.all { it >= MIN_PEAK }) { "bounces too low: $peaks" }
    val thrown = CHARGES - carried("minecraft:wind_charge")
    check(thrown == BOUNCES) { "$thrown charges thrown for $BOUNCES bounces" }
})

/** Heights above the ground the player reached before each landing. */
private fun GameTestScope.bouncePeaks(groundY: Double): List<Double> {
    val peaks = mutableListOf<Double>()
    var peak = 0.0
    var ticks = 0

    while (peaks.size < BOUNCES && ticks++ < BOUNCE_TIMEOUT_TICKS) {
        ticks(1)
        val (height, onGround) = client { client ->
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
