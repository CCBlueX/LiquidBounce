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
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.CrystalAuraPlaceTargetFactory
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.TIMEOUT
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.math.topCenter
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

/**
 * The CrystalAura Place/Scoring bonuses. Two mirrored obsidian blocks left and right of the husk deal the same
 * damage, so each bonus decides.
 */
class CrystalAuraScoringGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inCrystalAuraArena {
        scoring()
    }

    private fun CrystalAuraArena.scoring() {
        cmd("setblock ${pos(a)} crying_obsidian")
        cmd("setblock ${pos(left)} obsidian")
        cmd("setblock ${pos(right)} obsidian")
        configure(
            "Place/Enabled" to "false",
            "Destroy/Enabled" to "false",
            // walls may block all damage, which is still a valid tie for the bonuses to decide
            "Damage/MinEnemyDamage" to "0",
            "Damage/Efficient" to "false"
        )
        context.onClient { ModuleCrystalAura.enabled = true }
        context.waitFor({ client -> client.level!!.getBlockState(right).`is`(Blocks.OBSIDIAN) }, TIMEOUT)
        context.waitFor({ ModuleCrystalAura.targetTracker.target != null }, TIMEOUT)

        // yaw change
        var loser = other(pick())
        configure("Place/Scoring/YawChange" to "10")
        val towardLoser = context.fromClient { client ->
            Rotation.lookingAt(loser.topCenter, client.player!!.eyePosition).yaw
        }
        context.input.lookAt(towardLoser + side(loser) * -10f, 30f)
        context.waitTicks(3)
        check(pick() == loser) { "YawChange did not prefer the placement in view" }
        configure("Place/Scoring/YawChange" to "0")

        // a 1x1 hole, walls of crying obsidian, the side toward the husk walled for both
        for (spot in listOf(left, right)) {
            val crystal = spot.above()
            listOf(crystal.north(), crystal.south(), crystal.relative(sideTowardHusk(spot))).forEach {
                cmd("setblock ${pos(it)} crying_obsidian")
            }
        }
        context.waitTicks(2)
        loser = other(pick())
        val outer = loser.above().relative(sideTowardHusk(loser).opposite)
        cmd("setblock ${pos(outer)} crying_obsidian")
        context.waitFor({ client -> client.level!!.getBlockState(outer).`is`(Blocks.CRYING_OBSIDIAN) }, TIMEOUT)
        configure("Place/Scoring/OneByOneHole" to "10")
        check(pick() == loser) { "OneByOneHole did not prefer the crystal in the hole" }
    }

    /**
     * Runs the placement selection right now.
     */
    private fun CrystalAuraArena.pick(): BlockPos? = context.fromClient {
        CrystalAuraPlaceTargetFactory.updateTarget(null)
        CrystalAuraPlaceTargetFactory.placementTarget
    }

}
