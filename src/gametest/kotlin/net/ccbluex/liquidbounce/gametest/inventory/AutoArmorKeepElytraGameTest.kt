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
package net.ccbluex.liquidbounce.gametest.inventory

import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * AutoArmor keeps a worn elytra over a chestplate in the inventory unless KeepElytra is turned off.
 */
class AutoArmorKeepElytraGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.worldBuilder().create().use { world ->
            val bench = InventoryBench(context, world.server)
            bench.setUp()
            bench.checkElytraKept()
            bench.checkElytraReplaced()
        }
    }

    private fun InventoryBench.wearElytraWithChestplateInHotbar() {
        command("item replace entity @a armor.chest with minecraft:elytra")
        command("item replace entity @a hotbar.0 with minecraft:iron_chestplate")
    }

    private fun InventoryBench.wears(item: Item) =
        onServer { player -> player.getItemBySlot(EquipmentSlot.CHEST).`is`(item) }

    private fun InventoryBench.checkElytraKept() {
        wearElytraWithChestplateInHotbar()
        withModule(ModuleAutoArmor, {}) {
            context.waitTicks(SETTLE_TICKS)
            check(wears(Items.ELYTRA)) { "the worn elytra was replaced with KeepElytra on" }
        }
    }

    private fun InventoryBench.checkElytraReplaced() {
        wearElytraWithChestplateInHotbar()
        withModule(ModuleAutoArmor, { ModuleAutoArmor.set("KeepElytra", value = "false") }) {
            waitUntil(EQUIP_TICKS, { "the chestplate was not put on with KeepElytra off" }) {
                wears(Items.IRON_CHESTPLATE)
            }
            check(count(Items.ELYTRA) == 1) { "the replaced elytra did not stay in the inventory" }
        }
    }

    private companion object {
        const val SETTLE_TICKS = 40
        const val EQUIP_TICKS = 100
    }

}
