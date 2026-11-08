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
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * AutoWeapon takes the weapon SwitchDelay after the click, lands the click with it and puts the old item back
 * after SwitchBack. With TotemOnly it only switches away from a totem.
 */
class AutoWeaponDelaysGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.runCombatChecks(
        listOf(ModuleAutoWeapon),
        CombatScene::checkSwitchDelays,
        CombatScene::checkTotemOnly,
    )

}

private const val SWITCH_DELAY_MIN = 6
private const val SWITCH_DELAY_MAX = 8
private const val SWITCH_BACK_MIN = 10
private const val SWITCH_BACK_MAX = 12

// The click and the selected slot reach the server a few ticks after they were made
private const val LAG_TICKS = 4
private const val TIMEOUT_TICKS = 60
private const val IDLE_TICKS = 10
private const val STICK_DAMAGE = 1f

private fun CombatScene.giveStickAndSword() {
    world.server.runCommand("item replace entity @a hotbar.0 with minecraft:stick")
    world.server.runCommand("item replace entity @a hotbar.1 with minecraft:diamond_sword")
}

private fun CombatScene.clientTick(): Int = context.fromClient { checkNotNull(it.player).tickCount }

/**
 * Clicks the entity [id] under the crosshair.
 *
 * @return the client tick of the click
 */
private fun CombatScene.click(id: Int): Int {
    context.waitFor({ client -> crosshairEntity(client) == id }, CombatScene.ENTITY_TIMEOUT_TICKS)
    val tick = clientTick()
    context.input.holdKeyFor({ it.keyAttack }, 1)
    return tick
}

private fun CombatScene.serverSlot(): Int = onServer { serverPlayer(it).inventory.selectedSlot }

/**
 * Waits until the server has [slot] selected.
 *
 * @return the client tick by then
 */
private fun CombatScene.awaitServerSlot(slot: Int): Int {
    var ticks = 0
    while (serverSlot() != slot) {
        check(ticks++ < TIMEOUT_TICKS) { "AutoWeapon: the server never had slot $slot selected" }
        context.waitTick()
    }
    return clientTick()
}

private fun CombatScene.checkSwitchDelays() {
    reset()
    val husk = spawnHusk(at(0.0, 0.0, 2.0))
    giveStickAndSword()
    context.onClient {
        ModuleAutoWeapon.set("SwitchDelay", "$SWITCH_DELAY_MIN..$SWITCH_DELAY_MAX")
        ModuleAutoWeapon.set("SwitchBack", "$SWITCH_BACK_MIN..$SWITCH_BACK_MAX")
        ModuleAutoWeapon.enabled = true
    }
    context.waitTicks(ATTACK_TICKS)

    val clickedAt = click(husk)
    val switchedAt = awaitServerSlot(1)
    check(serverHealth(husk) == HUSK_HEALTH) { "AutoWeapon: the hit went out before the weapon was in hand" }
    check(switchedAt - clickedAt in SWITCH_DELAY_MIN..SWITCH_DELAY_MAX + LAG_TICKS) {
        "AutoWeapon: switched ${switchedAt - clickedAt} ticks after the click"
    }

    val switchedBackAt = awaitServerSlot(0)
    check(switchedBackAt - switchedAt in SWITCH_BACK_MIN..SWITCH_BACK_MAX + LAG_TICKS) {
        "AutoWeapon: switched back after ${switchedBackAt - switchedAt} ticks"
    }
    // A stick deals at most 1, so more means the click landed with the sword
    val damage = HUSK_HEALTH - serverHealth(husk)
    check(damage > STICK_DAMAGE) { "AutoWeapon: the delayed click dealt $damage" }
}

private fun CombatScene.checkTotemOnly() {
    reset()
    val husk = spawnHusk(at(0.0, 0.0, 2.0))
    giveStickAndSword()
    context.onClient {
        ModuleAutoWeapon.set("SwitchDelay", "0..0")
        ModuleAutoWeapon.set("TotemOnly", "true")
        ModuleAutoWeapon.enabled = true
    }
    context.waitTicks(ATTACK_TICKS)

    click(husk)
    val slots = List(IDLE_TICKS) {
        context.waitTick()
        serverSlot()
    }
    val damage = HUSK_HEALTH - serverHealth(husk)
    check(1 !in slots && damage > 0f && damage <= STICK_DAMAGE) {
        "AutoWeapon: TotemOnly switched away from a stick, server slots $slots, damage $damage"
    }

    world.server.runCommand("item replace entity @a hotbar.0 with minecraft:totem_of_undying")
    context.waitTicks(ATTACK_TICKS)
    click(husk)
    awaitServerSlot(1)
}
