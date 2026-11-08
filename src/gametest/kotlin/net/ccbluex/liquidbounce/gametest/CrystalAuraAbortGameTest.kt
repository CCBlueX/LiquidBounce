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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SubmoduleAbort
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.CrystalAuraPlaceTargetFactory
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.TIMEOUT
import net.ccbluex.liquidbounce.utils.math.topCenter
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.BlockPos

/**
 * The CrystalAura Abort options.
 */
class CrystalAuraAbortGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inCrystalAuraArena {
        abort()
    }

    private fun CrystalAuraArena.abort() {
        configure(
            "Destroy/Enabled" to "false",
            "Abort/Enabled" to "true",
            "Abort/MaxTargetMove" to "3",
            "Abort/DamageTolerance" to "10"
        )
        context.onClient { ModuleCrystalAura.enabled = true }
        context.awaitTicks(TIMEOUT) { crystalsOn(a) > 0 }

        // A plan is only checked between planning and acting; one planned and carried out within the same tick
        // cannot be outdated, so the decision is checked on a plan held across ticks
        configure("Place/Enabled" to "false")
        clearCrystals()
        val damage = context.fromClient { CrystalAuraPlaceTargetFactory.placementDamage }

        configure("Abort/MaxTargetMove" to "0")
        val plan = capturePlan(a, damage)
        check(context.fromClient { !plan.isOutdated() }) { "MaxTargetMove outdated a plan while nothing moved" }
        cmd("tp @e[type=husk,limit=1] ${huskPos.x + 0.5 + SHIFT} ${huskPos.y} ${huskPos.z + 0.5}")
        context.awaitTicks(TIMEOUT) { context.fromClient { plan.isOutdated() } }
        cmd("tp @e[type=husk,limit=1] ${huskPos.x + 0.5} ${huskPos.y} ${huskPos.z + 0.5}")
        context.awaitTicks(TIMEOUT) { context.fromClient { !plan.isOutdated() } }

        configure("Abort/MaxTargetMove" to "3")
        val worsePlan = capturePlan(a, damage + DAMAGE_OFFSET)
        val betterPlan = capturePlan(a, damage - DAMAGE_OFFSET)
        check(context.fromClient { !worsePlan.isOutdated() }) { "DamageTolerance 10 dropped a plan losing 5" }
        configure("Abort/DamageTolerance" to "2")
        check(context.fromClient { worsePlan.isOutdated() }) { "DamageTolerance 2 kept a plan losing 5" }
        check(context.fromClient { !betterPlan.isOutdated() }) { "DamageTolerance 2 dropped a plan gaining 5" }

        // The module still places with the husk standing still
        configure("Place/Enabled" to "true")
        context.awaitTicks(TIMEOUT) { crystalsOn(a) > 0 }
    }

    private fun CrystalAuraArena.capturePlan(base: BlockPos, damage: Float) = context.fromClient {
        checkNotNull(
            SubmoduleAbort.capture(base.topCenter, damage, CrystalAuraDamageOptions.RequestingSubmodule.PLACE)
        ) { "Abort captured no plan, no target" }
    }

    private companion object {
        const val SHIFT = 0.5
        const val DAMAGE_OFFSET = 5f
    }

}
