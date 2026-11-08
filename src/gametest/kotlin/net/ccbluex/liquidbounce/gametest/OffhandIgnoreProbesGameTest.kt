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
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffects

/**
 * The Offhand Totem/IgnoreProbes option: a totem popped by damage comes back at once, one popped by a probe only
 * after the delay.
 */
class OffhandIgnoreProbesGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        configure("Totem/IgnoreProbes/Enabled" to "true", "Totem/IgnoreProbes/Delay" to "$DELAY_MS..$DELAY_MS")
        giveTotems(3)
        context.onClient { ModuleOffhand.enabled = true }
        waitServer("a totem in the offhand") { it.offhandItem.isTotem() }

        val damage = popAndRefill { it.damageSources().magic() }
        check(damage < DELAY_MS / 2) { "The totem came back after ${damage}ms from a pop by damage" }

        // generic damage without a source, as an anticheat deals it to test the totem
        val probe = popAndRefill { it.damageSources().generic() }
        check(probe >= DELAY_MS - SLACK_MS) { "The totem came back after ${probe}ms from a probe" }
    }

    /**
     * Pops the offhand totem with damage from [source] and returns the milliseconds until it is refilled.
     */
    private fun OffhandFixture.popAndRefill(source: (ServerPlayer) -> DamageSource): Long {
        val popped = server.query { server ->
            val player = server.playerList.players[0]
            // Resistance V of the fixture blocks all damage
            player.removeEffect(MobEffects.RESISTANCE)
            player.removeEffect(MobEffects.ABSORPTION)
            player.absorptionAmount = 0f
            player.health = 1f
            // the last pop would cool the damage down
            player.damageCooldownTime = 0
            player.hurtServer(player.level(), source(player), LETHAL_DAMAGE)
            !player.offhandItem.isTotem()
        }
        check(popped) { "The totem did not pop" }
        val start = System.currentTimeMillis()
        waitServer("a refilled offhand") { it.offhandItem.isTotem() }
        return System.currentTimeMillis() - start
    }

    private companion object {
        const val DELAY_MS = 2000
        const val SLACK_MS = 100
        const val LETHAL_DAMAGE = 1000f
    }

}
