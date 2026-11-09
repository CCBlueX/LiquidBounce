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
package net.ccbluex.liquidbounce.gametest.combat

import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.EntityHitResult

/**
 * Looking down far enough that a husk stays under the crosshair for the whole jump.
 */
private const val PITCH = 30f
private const val JUMP_TICKS = 2
private const val LANDING_TICKS = 40

/**
 * Criticals Legit turns a ground click on a husk into a jump and hits once while falling, on whatever husk the
 * crosshair is on by then; with none under the crosshair the click is dropped.
 */
class CriticalsLegitGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleCriticals) { server, y ->
        val south = summonHusk(context, server, y, 0.5, 3.0)
        val north = summonHusk(context, server, y, 0.5, -2.0)
        context.onClient {
            ModuleCriticals.set("Mode", "Legit")
            ModuleCriticals.enabled = true
        }

        clickOn(context, server, y, south)
        context.onClient { client ->
            check(CombatEvents.attacks.isEmpty() && CombatEvents.punches == 0) { "Hit from the ground" }
            check(!checkNotNull(client.player).onGround()) { "Did not jump on the click" }
        }
        expectFallingHit(context, south)
        check(server.huskHealth(south) < HUSK_HEALTH) { "The falling hit did no damage" }

        // The hit knocked the husk out of reach
        server.discard(south)
        val moved = summonHusk(context, server, y, 0.5, 3.0)

        // Turns to the other husk during the jump
        clickOn(context, server, y, moved)
        context.onClient { client -> checkNotNull(client.player).yRot = 180f }
        expectFallingHit(context, north)
        check(server.huskHealth(moved) == HUSK_HEALTH) { "Hit the husk the click started on" }

        // Turns to nothing during the jump
        clickOn(context, server, y, moved)
        context.onClient { client -> checkNotNull(client.player).yRot = 90f }
        context.waitFor({ client -> checkNotNull(client.player).onGround() }, LANDING_TICKS)
        context.waitTicks(SETTLE_TICKS)
        val (attacks, punches) = context.fromClient { CombatEvents.attacks.size to CombatEvents.punches }
        check(attacks == 0 && punches == 0) { "Turned away: $attacks attacks, $punches swings" }
    }

    private fun clickOn(context: ClientGameTestContext, server: TestServerContext, y: Int, husk: Int) {
        face(context, server, y, 0f, PITCH)
        context.waitFor({ client -> (client.hitResult as? EntityHitResult)?.entity?.id == husk }, WAIT_TICKS)
        context.onClient { client ->
            CombatEvents.reset()
            KeyMapping.click(client.options.keyAttack.key)
        }
        context.waitTicks(JUMP_TICKS)
    }

    private fun expectFallingHit(context: ClientGameTestContext, husk: Int) {
        context.waitFor({ client -> checkNotNull(client.player).onGround() }, LANDING_TICKS)
        context.onClient {
            val hit = CombatEvents.attacks.map { it.id }
            check(hit == listOf(husk) && CombatEvents.fallingAttacks == 1) {
                "Expected one hit on $husk while falling: $hit, ${CombatEvents.fallingAttacks} falling"
            }
        }
    }

}
