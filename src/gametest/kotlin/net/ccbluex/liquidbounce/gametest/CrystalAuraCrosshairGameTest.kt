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
import net.ccbluex.liquidbounce.gametest.CrystalAuraArena.Companion.TIMEOUT
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.gametest.harness.LocalGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

/**
 * Looking at an obsidian block beside the husk, the CrystalAura Crosshair rotation mode places and breaks a crystal
 * there without rotating, although the block in front of the husk would deal more damage.
 */
class CrystalAuraCrosshairGameTest : LocalGameTest() {

    override fun test(context: ClientGameTestContext) = context.inCrystalAuraArena {
        cmd("setblock ${pos(right)} obsidian")
        context.waitFor({ client -> client.level!!.getBlockState(right).`is`(Blocks.OBSIDIAN) }, TIMEOUT)

        val view = context.fromClient { client ->
            Rotation.lookingAt(Vec3.upFromBottomCenterOf(right, 1.0), client.player!!.eyePosition)
        }
        context.input.lookAt(view.yaw, view.pitch)
        context.waitTicks(2)
        val before = serverRotation()

        configure("RotationMode" to "Crosshair")
        context.onClient { ModuleCrystalAura.enabled = true }

        // the next crystal can replace a broken one before a tick samples the gap, so follow the first one
        var first: Int? = null
        val broken = context.ticksUntil(TIMEOUT) {
            check(crystalsOn(a) == 0) { "Crosshair mode placed on a block the player does not look at" }
            check(serverRotation().isCloseTo(before)) { "Crosshair mode rotated: $before -> ${serverRotation()}" }

            val ids = crystalIdsOn(right)
            first = first ?: ids.firstOrNull()
            first != null && first !in ids
        }

        check(first != null && broken) { "Crosshair mode placed=${first != null} broken=$broken on that block" }
    }

}
