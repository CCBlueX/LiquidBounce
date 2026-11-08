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

import net.ccbluex.liquidbounce.features.module.modules.combat.autoclicker.ModuleAutoClicker
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.entity.boss.enderdragon.EndCrystal

private const val JUMP_PITCH = 25f
private const val JUMP_TICKS = 40
private const val SKY_PITCH = -90f

/**
 * While the attack key is held, AutoClicker's crystal objective skips husks, and the ground rule keeps its clicks
 * on crystals to the ground.
 */
class AutoClickerCrystalsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleAutoClicker) { server, y ->
        context.onClient {
            ModuleAutoClicker.set("Attack/Objective", "Crystal")
            ModuleAutoClicker.set("Attack/Criticals", "Ignore")
            ModuleAutoClicker.set("Attack/Clicker/Technique", "Constant")
            ModuleAutoClicker.set("Attack/Clicker/CPS", "20..20")
            ModuleAutoClicker.enabled = true
        }

        // The press itself attacks like any click, so it goes to the sky
        face(context, server, y, 0f, SKY_PITCH)
        context.input.holdKey { it.keyAttack }
        try {
            context.waitTicks(SETTLE_TICKS)
            checkObjective(context, server, y)
            checkGroundRule(context, server, y)
        } finally {
            context.input.releaseKey { it.keyAttack }
            context.onClient { ModuleAutoClicker.enabled = false }
        }
    }

    private fun checkObjective(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val husk = summonHusk(context, server, y, 0.5, 2.0)
        face(context, server, y, 0f)
        context.onClient { CombatEvents.reset() }
        context.waitTicks(MEASURE_TICKS / 2)
        check(context.fromClient { CombatEvents.attacks.isEmpty() }) { "Clicked a husk with the crystal objective" }
        server.discard(husk)
    }

    private fun checkGroundRule(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val crystal = summonCrystal(context, server, y, 0.5, 3.0)
        face(context, server, y, 0f, JUMP_PITCH)
        context.waitFor({ CombatEvents.attacks.any { it is EndCrystal } }, WAIT_TICKS)

        fun airborneClicks(): Int {
            context.onClient { CombatEvents.reset() }
            context.input.holdKey { it.keyJump }
            context.waitTicks(JUMP_TICKS)
            context.input.releaseKey { it.keyJump }
            context.waitFor({ client -> client.player?.onGround() == true }, WAIT_TICKS)
            return context.fromClient { CombatEvents.airborneAttacks }
        }

        context.onClient { ModuleAutoClicker.set("Attack/CrystalsOnlyOnGround", "true") }
        val grounded = airborneClicks()
        context.onClient { ModuleAutoClicker.set("Attack/CrystalsOnlyOnGround", "false") }
        val free = airborneClicks()
        check(grounded == 0 && free > 0) { "Crystal clicks in the air: $grounded with the ground rule, $free without" }

        // Killing it would blow it up next to the player
        server.discard(crystal)
    }

}
