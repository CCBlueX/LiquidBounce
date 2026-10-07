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

import com.google.gson.Gson
import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoRod
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** Checks the real guard shared by rod targeting and use, with no nearby enemies. */
class AutoRodRequirementsGameTest : FabricClientGameTest {

    private val requirements = ModuleAutoRod::class.java.getDeclaredMethod("getRequirementsMet")
        .apply { isAccessible = true }
    private val blockers = listOf(ModuleBlink, ModuleScaffold, ModuleFreeze)

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleAutoRod) }
        val blockerStates = context.fromClient { blockers.map { it.enabled } }

        try {
            context.worldBuilder().create().use { world ->
                world.server.runCommand("gamemode creative @a")
                context.waitFor({ client -> client.player?.hasInfiniteMaterials() == true }, 200)
                context.onClient {
                    blockers.forEach { it.enabled = false }
                    ModuleAutoRod.settings.getValue("Requires").deserializeFrom(Gson(), JsonArray())
                    ModuleAutoRod.settings.getValue("Ignore").deserializeFrom(Gson(), JsonArray())
                    ModuleAutoRod.settings.getValue("MinHealth").setByString("10")
                    ModuleAutoRod.enabled = true
                    check(ModuleAutoRod.running)

                    // Exercise both the default cap and the unlimited setting.
                    for (limit in listOf(1, 0)) {
                        ModuleAutoRod.settings.getValue("MaxEnemiesNearby").setByString(limit.toString())
                        prepareInventory()
                        assertAllowed(true, "healthy player with a rod, limit $limit")
                        assertHealthGuard()
                        assertInventoryGuards()
                        assertConfiguredGuards()
                        assertModuleGuards()
                    }
                }
            }
        } finally {
            context.onClient {
                ModuleAutoRod.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleAutoRod, saved)
                blockers.zip(blockerStates).forEach { (module, enabled) -> module.enabled = enabled }
            }
        }
    }

    private fun prepareInventory() {
        val player = checkNotNull(mc.player)
        player.health = 20f
        player.inventory.clearContent()
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.DIAMOND_SWORD))
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack(Items.FISHING_ROD))
        EventManager.callEvent(GameTickEvent)
    }

    private fun assertHealthGuard() {
        val player = checkNotNull(mc.player)
        player.health = 8f
        assertAllowed(false, "health below MinHealth")
        player.health = 20f
    }

    private fun assertInventoryGuards() {
        val player = checkNotNull(mc.player)
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
        EventManager.callEvent(GameTickEvent)
        assertAllowed(false, "no fishing rod")
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack(Items.FISHING_ROD))
        EventManager.callEvent(GameTickEvent)

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.BOW))
        assertAllowed(false, "holding an ignored item")
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.DIAMOND_SWORD))
    }

    private fun assertConfiguredGuards() {
        val player = checkNotNull(mc.player)
        val requires = ModuleAutoRod.settings.getValue("Requires")
        requires.deserializeFrom(Gson(), JsonArray().apply { add("Weapon") })
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.STONE))
        assertAllowed(false, "the configured Weapon requirement")
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.DIAMOND_SWORD))
        requires.deserializeFrom(Gson(), JsonArray())

        val ignores = ModuleAutoRod.settings.getValue("Ignore")
        ignores.deserializeFrom(Gson(), JsonArray().apply { add("HoldingConsumable") })
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.APPLE))
        assertAllowed(false, "the configured HoldingConsumable ignore")
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.DIAMOND_SWORD))
        ignores.deserializeFrom(Gson(), JsonArray())
    }

    private fun assertModuleGuards() {
        for (module in blockers) {
            module.enabled = true
            try {
                check(module.running) { "${module.name} did not start" }
                assertAllowed(false, "${module.name} is running")
            } finally {
                module.enabled = false
            }
        }
        assertAllowed(true, "all guards satisfied again")
    }

    private fun assertAllowed(expected: Boolean, case: String) {
        val actual = requirements.invoke(ModuleAutoRod) as Boolean
        check(actual == expected) { "$case: expected $expected, got $actual" }
    }

}
