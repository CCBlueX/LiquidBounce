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

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleHitbox
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import kotlin.math.floor

/**
 * Hitbox only enlarges the targets its Range, ThroughWalls and IgnoreShield settings let through.
 */
class HitboxFiltersGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) =
        context.runCombatChecks(listOf(ModuleHitbox), CombatScene::checkHitboxFilters)

}

private const val SETTLE_TICKS = 5

private fun CombatScene.expectCrosshair(expected: Int?, message: String) {
    context.waitTicks(SETTLE_TICKS)
    check(context.fromClient { crosshairEntity(it) } == expected) { "Hitbox: $message" }
}

private fun CombatScene.checkHitboxFilters() {
    reset()
    // Looking along +Z misses the real box by 0.7 blocks, its nearest point is 1.84 blocks from the eyes
    val husk = spawnHusk(at(1.0, 0.0, 2.0))
    expectCrosshair(null, "the real box is already under the crosshair")

    context.onClient {
        ModuleHitbox.set("Size", "1")
        ModuleHitbox.enabled = true
    }
    expectCrosshair(husk, "the enlarged box is not under the crosshair")

    context.onClient { ModuleHitbox.set("Range", "1.5") }
    expectCrosshair(null, "enlarged a target out of range")
    context.onClient { ModuleHitbox.set("Range", "2") }
    expectCrosshair(husk, "did not enlarge a target in range")

    context.onClient { ModuleHitbox.set("ThroughWalls", "false") }
    expectCrosshair(husk, "did not enlarge a target in plain sight")
    // Hides the husk and leaves the crosshair ray free
    val x = floor(origin.x).toInt() + 1
    val y = floor(origin.y).toInt()
    val z = floor(origin.z).toInt() + 1
    world.server.runCommand("fill $x $y $z ${x + 2} ${y + 2} $z minecraft:stone")
    expectCrosshair(null, "enlarged a target behind a wall")
    context.onClient { ModuleHitbox.set("ThroughWalls", "true") }
    expectCrosshair(husk, "did not enlarge a target behind a wall")

    raiseShield(husk)
    expectCrosshair(husk, "did not enlarge a target blocking with a shield")
    context.onClient { ModuleHitbox.set("IgnoreShield", "false") }
    expectCrosshair(null, "enlarged a target blocking with a shield")
}
