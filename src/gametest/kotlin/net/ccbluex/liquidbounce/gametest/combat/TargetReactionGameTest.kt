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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.util.Mth
import kotlin.math.abs

/**
 * With the reaction of their target, KillAura neither turns to a new target nor hits it for the reaction time and
 * Aimbot leaves the view where it is. The target KillAura had before costs no reaction again.
 */
class TargetReactionGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) =
        context.combatTest(ModuleKillAura, ModuleAimbot) { server, y ->
            checkKillAura(context, server, y)
            checkAimbot(context, server, y)
        }

    private fun checkKillAura(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val first = summonHusk(context, server, y, 3.5, 0.5)
        val (_, immediate) = engage(context)
        server.discard(first)

        context.reactSlowly(ModuleKillAura)
        val second = summonHusk(context, server, y, -2.5, 0.5)
        val (turned, delayed) = engage(context)
        val (again, _) = engage(context)
        server.discard(second)
        logger.info("KillAura attacked after $immediate ticks, with the reaction it turned after $turned and " +
            "attacked after $delayed ticks, and turned to the same target again after $again ticks")

        check(immediate < REACTION_TICKS && turned >= REACTION_TICKS && delayed >= turned) {
            "KillAura attacked after $immediate ticks without a reaction, turned after $turned and attacked " +
                "after $delayed ticks with a reaction of $REACTION_TICKS ticks"
        }
        check(again < REACTION_TICKS) { "KillAura took $again ticks to turn to the target it had before" }
    }

    /**
     * Enables KillAura with a husk beside the player, who faces south.
     *
     * @return the ticks until the rotation left the south, and until the first attack
     */
    private fun engage(context: ClientGameTestContext): Pair<Int, Int> {
        context.onClient {
            CombatEvents.reset()
            ModuleKillAura.enabled = true
        }

        var turned = -1
        var attacked = -1
        for (tick in 0 until WAIT_TICKS) {
            val (yaw, attacks) = context.fromClient {
                (RotationManager.currentRotation?.yaw ?: 0f) to CombatEvents.attacks.size
            }
            if (turned < 0 && abs(Mth.wrapDegrees(yaw)) > KILL_AURA_TURN) turned = tick
            if (attacks > 0) {
                attacked = tick
                break
            }
            context.waitTick()
        }

        context.onClient { ModuleKillAura.enabled = false }
        context.waitFor({ RotationManager.currentRotation == null }, WAIT_TICKS)
        check(turned >= 0 && attacked >= 0) { "KillAura did not attack within $WAIT_TICKS ticks" }
        return turned to attacked
    }

    private fun checkAimbot(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        val immediate = ticksToTurn(context, server, y)

        context.reactSlowly(ModuleAimbot)
        val delayed = ticksToTurn(context, server, y)
        logger.info("Aimbot turned after $immediate ticks, with the reaction after $delayed ticks")

        check(immediate < REACTION_TICKS && delayed >= REACTION_TICKS) {
            "Aimbot turned after $immediate ticks without a reaction and after $delayed ticks with a reaction " +
                "of $REACTION_TICKS ticks"
        }
    }

    /**
     * Enables Aimbot with a husk diagonally in front of the player, who faces south.
     *
     * @return the ticks until the view left the south
     */
    private fun ticksToTurn(context: ClientGameTestContext, server: TestServerContext, y: Int): Int {
        val husk = summonHusk(context, server, y, 2.5, 2.5)
        face(context, server, y, 0f)
        context.onClient { ModuleAimbot.enabled = true }

        var turned = -1
        for (tick in 0 until WAIT_TICKS) {
            val yaw = context.fromClient { checkNotNull(it.player).yRot }
            if (abs(Mth.wrapDegrees(yaw)) > AIMBOT_TURN) {
                turned = tick
                break
            }
            context.waitTick()
        }

        context.onClient { ModuleAimbot.enabled = false }
        server.discard(husk)
        check(turned >= 0) { "Aimbot did not turn within $WAIT_TICKS ticks" }
        return turned
    }

    private fun ClientGameTestContext.reactSlowly(module: ClientModule) = onClient {
        module.set("Target/Reaction/Delay", "$REACTION_TICKS..$REACTION_TICKS")
        module.set("Target/Reaction/Enabled", "true")
    }

    private companion object {
        const val KILL_AURA_TURN = 45f
        const val AIMBOT_TURN = 10f
    }

}
