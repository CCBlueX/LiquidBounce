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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.Anticheat
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.configure
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.restoring
import net.ccbluex.liquidbounce.utils.inventory.InventoryRequirements
import net.minecraft.world.item.Items

private const val LOOT_CHEST =
    """minecraft:chest{Items:[{Slot:3b,id:"minecraft:diamond_sword",count:1},""" +
        """{Slot:12b,id:"minecraft:diamond_chestplate",count:1},{Slot:20b,id:"minecraft:arrow",count:16}]}"""

/**
 * [ModuleInventoryCleaner], [ModuleAutoArmor] and [ModuleChestStealer] with their default delays on a real server:
 * what they click has to arrive there, and the anticheat reports nothing.
 *
 * Grim objects to inventory clicks of a player that gives movement input and takes them back. With `NoMovement`
 * required the modules have to wait for the input to end instead.
 */
class InventoryModulesGameTest : ServerGameTest({
    context.restoring(ModuleInventoryCleaner, ModuleAutoArmor, ModuleChestStealer) {
        giveLoadout()
        enable(ModuleAutoArmor)
        enable(ModuleInventoryCleaner)
        awaitSorted()
        disable(ModuleInventoryCleaner)
        disable(ModuleAutoArmor)

        if (Anticheat.GRIM in anticheats) {
            giveLoadout()
            whileJumping {
                expectViolation("a click while jumping") {
                    enable(ModuleAutoArmor)
                    ticks(30)
                    disable(ModuleAutoArmor)
                }
            }
        }

        requireNoMovement(ModuleAutoArmor)
        requireNoMovement(ModuleInventoryCleaner)
        giveLoadout()
        whileJumping {
            enable(ModuleAutoArmor)
            enable(ModuleInventoryCleaner)
            ticks(40)
            check(carried("minecraft:glass_bottle") == 1 && carried("minecraft:stone_sword") == 1) {
                "InventoryCleaner clicked while the player was jumping"
            }
            awaitServer("items entity $playerName armor.chest minecraft:leather_chestplate")
        }
        awaitSorted()
        disable(ModuleInventoryCleaner)
        disable(ModuleAutoArmor)

        lootsChest()
    }
})

private fun GameTestScope.giveLoadout() {
    resetInventory()
    setSlot("hotbar.0", "minecraft:stone_sword")
    setSlot("hotbar.3", "minecraft:arrow", 16)
    setSlot("inventory.0", "minecraft:iron_sword")
    setSlot("inventory.1", "minecraft:glass_bottle")
    setSlot("inventory.2", "minecraft:iron_chestplate")
    setSlot("armor.chest", "minecraft:leather_chestplate")
    awaitClient("the items") { checkNotNull(it.player).inventory.countItem(Items.IRON_CHESTPLATE) == 1 }
}

private fun GameTestScope.awaitSorted() {
    awaitServer("items entity $playerName armor.chest minecraft:iron_chestplate")
    awaitServer("items entity $playerName hotbar.0 minecraft:iron_sword")
    awaitServer("items entity $playerName inventory.* minecraft:arrow")
    awaitJunkGone("minecraft:stone_sword", "minecraft:glass_bottle", "minecraft:leather_chestplate")
    check(carried("minecraft:arrow") == 16) { "arrows went missing while they were moved out of the hotbar" }
}

private inline fun GameTestScope.whileJumping(block: () -> Unit) {
    input.holdKey { it.keyJump }
    try {
        awaitClient("the jump") { checkNotNull(it.player).jumping }
        block()
    } finally {
        input.releaseKey { it.keyJump }
    }
}

private fun GameTestScope.requireNoMovement(module: ClientModule) = configure(
    module.settings.getValue("Constraints") as ValueGroup,
    "Requires" to enumSetOf(InventoryRequirements.NO_MOVEMENT),
)

private fun GameTestScope.lootsChest() {
    resetInventory()
    setSlot("hotbar.0", "minecraft:iron_sword")
    setSlot("armor.chest", "minecraft:iron_chestplate")
    awaitClient("the items") { checkNotNull(it.player).inventory.countItem(Items.IRON_SWORD) == 1 }

    val chestPos = origin.south(2)
    setBlock(chestPos, LOOT_CHEST)
    awaitServer("block ${chestPos.x} ${chestPos.y} ${chestPos.z} minecraft:chest")

    enable(ModuleAutoArmor)
    enable(ModuleInventoryCleaner)
    enable(ModuleChestStealer)
    awaitServer("items entity $playerName armor.chest minecraft:diamond_chestplate")
    awaitServer("items entity $playerName hotbar.0 minecraft:diamond_sword")
    awaitJunkGone("minecraft:iron_sword", "minecraft:iron_chestplate")
    check(carried("minecraft:arrow") == 16) { "the arrows of the chest did not arrive" }

    disable(ModuleChestStealer)
    disable(ModuleInventoryCleaner)
    disable(ModuleAutoArmor)
    setBlock(chestPos, "minecraft:air")
}

/** Thrown items lie at the player's feet and would be picked up again after two seconds. */
private fun GameTestScope.awaitJunkGone(vararg items: String) {
    repeat(40) {
        run("execute at $playerName run kill @e[type=item,distance=..8]")
        if (items.all { carried(it) == 0 }) {
            return
        }
        ticks(5)
    }
    throw AssertionError("The player still carries one of ${items.joinToString()}, see ${server.log}")
}

private fun GameTestScope.resetInventory() {
    run("clear $playerName")
    run("execute at $playerName run kill @e[type=item,distance=..8]")
    awaitClient("an empty inventory") { checkNotNull(it.player).inventory.isEmpty }
}

private fun GameTestScope.setSlot(slot: String, item: String, count: Int = 1) {
    run("item replace entity $playerName $slot with $item $count")
}
