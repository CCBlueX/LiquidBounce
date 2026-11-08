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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.SHORT_TIMEOUT
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.TIMEOUT
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.YAW_TOLERANCE
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.Mth
import kotlin.math.abs

/**
 * The CrystalAura options for the block the last crystal was placed on: Destroy/PitchOnlyOnLastBase and
 * BreakLastOnDisable.
 */
class CrystalAuraLastBaseGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inCrystalAuraArena {
        pitchOnlyOnLastBase()
        breakLastOnDisable()
    }

    /**
     * Places on A, waits for the rotation to return to the player's, which misses the crystal, then breaks it.
     */
    private fun CrystalAuraArena.pitchOnlyOnLastBase() {
        context.input.lookAt(LOOK_YAW, LOOK_PITCH)
        configure("Destroy/Enabled" to "false", "Place/Enabled" to "true", "Destroy/PitchOnlyOnLastBase" to "true")
        context.onClient { ModuleCrystalAura.enabled = true }
        context.awaitTicks(TIMEOUT) { crystalsOn(a) > 0 }
        configure("Place/Enabled" to "false")
        // a fixed wait is not enough under load, the yaw must be back at the player's before it is measured
        context.awaitTicks(TIMEOUT) { abs(Mth.wrapDegrees(serverRotation().first - LOOK_YAW)) <= EXACT }

        val before = serverRotation()
        configure("Destroy/Enabled" to "true")
        var yawChange = 0f
        var pitchChange = 0f
        val broken = context.ticksUntil(TIMEOUT) {
            val (yaw, pitch) = serverRotation()
            yawChange = maxOf(yawChange, abs(Mth.wrapDegrees(yaw - before.first)))
            pitchChange = maxOf(pitchChange, abs(pitch - before.second))
            crystalsOn(a) == 0
        }

        check(broken) { "The crystal on the last base was not broken" }
        check(yawChange <= EXACT && pitchChange > YAW_TOLERANCE) {
            "PitchOnlyOnLastBase changed the yaw by $yawChange and the pitch by $pitchChange"
        }
        configure("Destroy/PitchOnlyOnLastBase" to "false")
    }

    private fun CrystalAuraArena.breakLastOnDisable() {
        configure("Destroy/Enabled" to "false", "Place/Enabled" to "true", "BreakLastOnDisable" to "true")
        context.awaitTicks(TIMEOUT) { crystalsOn(a) > 0 }
        context.waitTicks(5)
        check(crystalsOn(a) > 0) { "The crystal broke without Destroy" }

        context.onClient { ModuleCrystalAura.enabled = false }
        context.awaitTicks(SHORT_TIMEOUT) { crystalsOn(a) == 0 }
        configure("BreakLastOnDisable" to "false", "Destroy/Enabled" to "true")
    }

    private companion object {
        const val EXACT = 0.01f
        const val LOOK_YAW = 12f

        // looks over the crystal, so breaking it needs another pitch
        const val LOOK_PITCH = -30f
    }

}
