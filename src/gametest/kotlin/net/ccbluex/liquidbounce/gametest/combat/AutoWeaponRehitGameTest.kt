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

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * A click that makes AutoWeapon take the sword still hits like the totem held before. With Rehit the sword hits
 * again a tick later.
 */
class AutoWeaponRehitGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) =
        context.runCombatChecks(listOf(ModuleAutoWeapon), CombatScene::checkRehit)

}

private const val FULL_CHARGE_TICKS = 20
private const val HIT_TICKS = 10

/**
 * Clicks a husk once with a totem in hand and a sword next to it.
 *
 * @return the damage the husk took
 */
private fun CombatScene.clickWithTotem(rehit: Boolean): Float {
    reset()
    val husk = spawnHusk(at(0.0, 0.0, 2.0))
    world.server.runCommand("item replace entity @a hotbar.0 with minecraft:totem_of_undying")
    world.server.runCommand("item replace entity @a hotbar.1 with minecraft:diamond_sword")
    context.onClient {
        ModuleAutoWeapon.set("Rehit", rehit.toString())
        ModuleAutoWeapon.enabled = true
    }
    context.waitTicks(FULL_CHARGE_TICKS)

    context.waitFor({ client -> crosshairEntity(client) == husk }, CombatScene.ENTITY_TIMEOUT_TICKS)
    context.input.holdKeyFor({ it.keyAttack }, 1)
    context.waitTicks(HIT_TICKS)
    context.onClient { ModuleAutoWeapon.enabled = false }

    return HUSK_HEALTH - serverHealth(husk)
}

private fun CombatScene.checkRehit() {
    val damage = clickWithTotem(rehit = false)
    val rehitDamage = clickWithTotem(rehit = true)

    // Within the hurt time of the first hit a second one only counts when it is stronger
    check(damage > 0f && rehitDamage > damage) {
        "AutoWeapon: the click dealt $damage, with Rehit $rehitDamage"
    }
}
