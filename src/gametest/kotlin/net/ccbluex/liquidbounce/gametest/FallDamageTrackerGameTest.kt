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

import net.ccbluex.liquidbounce.utils.network.LocalPlayerFallDamageTracker
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.Minecraft

/**
 * A hurt cycle from fall damage must be told apart from one from an attack, see [LocalPlayerFallDamageTracker].
 *
 * The flat world of the test API is survival, spawns no mobs and has solid ground everywhere, so both cycles can be
 * triggered by server commands on a player that stays alive.
 */
class FallDamageTrackerGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode survival @a")
            context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)

            assertFallDamageIsTracked(context, world)
            assertWindowExpires(context)
            assertOtherDamageIsNotTracked(context, world)
        }
    }

    private fun assertFallDamageIsTracked(context: ClientGameTestContext, world: TestSingleplayerContext) {
        // 8 blocks, hurting for 5 - the player has to survive the rest of the test
        world.server.runCommand("tp @a ~ ~8 ~")

        val waited = context.waitFor(::hurt, SETTLE_TIMEOUT_TICKS)
        check(context.fromClient { LocalPlayerFallDamageTracker.isCurrentFallDamage }) {
            "fall damage is not tracked as such, landed after $waited ticks"
        }
    }

    private fun assertWindowExpires(context: ClientGameTestContext) {
        // Past the HURT_TICKS window, and past the vanilla damage cooldown the next step would be swallowed by
        context.waitTicks(DAMAGE_COOLDOWN_TICKS)

        check(!context.fromClient { LocalPlayerFallDamageTracker.isCurrentFallDamage }) {
            "the hurt window outlives the hurt cycle it belongs to"
        }
    }

    private fun assertOtherDamageIsNotTracked(context: ClientGameTestContext, world: TestSingleplayerContext) {
        world.server.runCommand("damage @p 1 minecraft:player_attack")

        val waited = context.waitFor(::hurt, SETTLE_TIMEOUT_TICKS)
        check(!context.fromClient { LocalPlayerFallDamageTracker.isCurrentFallDamage }) {
            "an attack is tracked as fall damage, hurt after $waited ticks"
        }
    }

    private fun hurt(client: Minecraft): Boolean = (client.player?.hurtTime ?: 0) > 0

    private companion object {
        const val SETTLE_TIMEOUT_TICKS = 200
        const val DAMAGE_COOLDOWN_TICKS = 20
    }

}
