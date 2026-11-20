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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.action.aim
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReplenish
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.configure
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import java.util.concurrent.atomic.AtomicInteger

// Eaten at once and at any hunger, the way soup is on the servers AutoSoup is made for
private const val INSTANT_STEW =
    "minecraft:mushroom_stew[minecraft:consumable={consume_seconds:0}," +
        "minecraft:food={nutrition:6,saturation:7.2,can_always_eat:true}]"

/**
 * Inventory plans of modules that work on the hotbar and the offhand:
 * 1. `Offhand` swaps a totem from the inventory into the offhand.
 * 2. `Replenish` refills every low hotbar stack in one plan.
 * 3. `AutoTool` takes the tool from the inventory while mining and puts it back afterwards.
 * 4. `AutoBuff` `Soup` moves the empty bowl out of the hotbar.
 */
class InventoryHotbarPlansGameTest : SingleplayerGameTest({
    context.restoring(ModuleOffhand, ModuleReplenish, ModuleAutoTool, ModuleAutoBuff) {
        run("gamemode survival $playerName")
        awaitClient("survival mode") { checkNotNull(it.player).hasInfiniteMaterials().not() }

        offhandTakesTotemFromInventory()
        replenishRefillsEveryLowHotbarStack()
        autoToolTakesToolFromInventoryAndPutsItBack()
        soupStacksEmptyBowlInInventory()
    }
})

private fun GameTestScope.offhandTakesTotemFromInventory() {
    resetInventory()
    useFastConstraints(ModuleOffhand.settings.getValue("Constraints") as ValueGroup)

    setSlot("inventory.0", "minecraft:totem_of_undying")
    awaitClient("the totem") { checkNotNull(it.player).inventory.countItem(Items.TOTEM_OF_UNDYING) == 1 }

    countingClicks { clicks ->
        // The Totem defaults want one as long as an armor slot is empty
        enable(ModuleOffhand)
        awaitServer("items entity $playerName weapon.offhand minecraft:totem_of_undying")
        awaitClient("the totem in the offhand") { checkNotNull(it.player).offhandItem.`is`(Items.TOTEM_OF_UNDYING) }
        check(client { checkNotNull(it.player).inventory.getItem(9).isEmpty }) {
            "the totem is still in the inventory slot it was taken from"
        }
        check(client { checkNotNull(it.player).inventory.countItem(Items.TOTEM_OF_UNDYING) == 1 }) {
            "the totem was lost or duplicated by the switch"
        }
        checkNoFurtherClicks(clicks, "Offhand")
    }
    disable(ModuleOffhand)
}

private fun GameTestScope.replenishRefillsEveryLowHotbarStack() {
    resetInventory()
    useFastConstraints(ModuleReplenish.settings.getValue("Constraints") as ValueGroup)

    setSlot("hotbar.0", "minecraft:cobblestone", 3)
    setSlot("hotbar.1", "minecraft:oak_planks", 2)
    setSlot("inventory.0", "minecraft:cobblestone", 40)
    setSlot("inventory.1", "minecraft:oak_planks", 30)
    awaitClient("the low stacks and their refills") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.COBBLESTONE) == 43 && inv.countItem(Items.OAK_PLANKS) == 32
    }

    countingClicks { clicks ->
        enable(ModuleReplenish)
        awaitServer("items entity $playerName hotbar.0 minecraft:cobblestone[count=43]")
        awaitServer("items entity $playerName hotbar.1 minecraft:oak_planks[count=32]")
        awaitClient("both hotbar stacks refilled") {
            val inv = checkNotNull(it.player).inventory
            inv.getItem(0).count == 43 && inv.getItem(1).count == 32
        }
        check(client {
            val player = checkNotNull(it.player)
            player.inventory.getItem(9).isEmpty && player.inventory.getItem(10).isEmpty &&
                player.containerMenu.carried.isEmpty
        }) { "the refill stacks were not used up, or one is left on the cursor" }
        check(carried("minecraft:cobblestone") == 43 && carried("minecraft:oak_planks") == 32) {
            "items were lost while the hotbar was refilled"
        }
        checkNoFurtherClicks(clicks, "Replenish")
    }
    disable(ModuleReplenish)
}

private fun GameTestScope.autoToolTakesToolFromInventoryAndPutsItBack() {
    resetInventory()
    val constraints = client {
        val considerInventory = ModuleAutoTool.toolSelector.activeMode.inner
            .first { it.name == "ConsiderInventory" } as ToggleableValueGroup
        considerInventory.enabled = true
        considerInventory.inner.first { it.name == "Constraints" } as ValueGroup
    }
    useFastConstraints(constraints)
    configure(ModuleAutoTool, "SwapBackDelay" to 10)

    // With a full hotbar, the tool is swapped into the selected slot
    val selected = client { checkNotNull(it.player).inventory.selectedSlot }
    for (slot in 0..8) {
        setSlot("hotbar.$slot", if (slot == selected) "minecraft:stick" else "minecraft:dirt")
    }
    setSlot("inventory.0", "minecraft:diamond_pickaxe")
    awaitClient("the full hotbar and the pickaxe") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.DIAMOND_PICKAXE) == 1 && inv.countItem(Items.DIRT) == 8
    }

    // Obsidian outlasts the test even with the pickaxe
    val blockPos = origin.south().above()
    setBlock(blockPos, "minecraft:obsidian")
    awaitServer("block ${blockPos.x} ${blockPos.y} ${blockPos.z} minecraft:obsidian")
    aim(blockPos)
    awaitClient("the obsidian under the crosshair") { (it.hitResult as? BlockHitResult)?.blockPos == blockPos }

    countingClicks { clicks ->
        enable(ModuleAutoTool)
        client { it.mouseHandler.grabMouse() }
        input.holdKey { it.keyAttack }
        try {
            awaitServer("items entity $playerName hotbar.$selected minecraft:diamond_pickaxe")
            // Longer than SwapBackDelay
            ticks(20)
            check(client {
                val inv = checkNotNull(it.player).inventory
                inv.getItem(selected).`is`(Items.DIAMOND_PICKAXE) && inv.getItem(9).`is`(Items.STICK)
            }) { "the pickaxe did not stay in the hotbar while mining went on" }
        } finally {
            input.releaseKey { it.keyAttack }
        }

        awaitServer("items entity $playerName hotbar.$selected minecraft:stick")
        awaitServer("items entity $playerName inventory.0 minecraft:diamond_pickaxe")
        awaitClient("the stick back in its slot") {
            val inv = checkNotNull(it.player).inventory
            inv.getItem(selected).`is`(Items.STICK) && inv.getItem(9).`is`(Items.DIAMOND_PICKAXE)
        }
        check(clicks.get() == 2) { "expected one swap and one swap back, AutoTool sent ${clicks.get()} clicks" }
        checkNoFurtherClicks(clicks, "AutoTool")
    }
    disable(ModuleAutoTool)
    setBlock(blockPos, "minecraft:air")
}

private fun GameTestScope.soupStacksEmptyBowlInInventory() {
    resetInventory()
    val constraints = client {
        val soup = ModuleAutoBuff.settings.getValue("Soup") as ValueGroup
        val afterUse = soup.inner.first { it.name == "AfterUse" } as ModeValueGroup<*>
        afterUse.setByString("Stack")
        afterUse.activeMode.inner.first { it.name == "Constraints" } as ValueGroup
    }
    useFastConstraints(constraints)

    setSlot("hotbar.4", INSTANT_STEW)
    awaitClient("the stew") { checkNotNull(it.player).inventory.getItem(4).`is`(Items.MUSHROOM_STEW) }

    countingClicks { clicks ->
        enable(ModuleAutoBuff)
        ticks(5)
        check(carried("minecraft:mushroom_stew") == 1) { "the soup was eaten at full health" }

        // Soup is eaten at 40% of the health or less
        run("damage $playerName 16")
        awaitCondition("the soup eaten") { carried("minecraft:mushroom_stew") == 0 }
        awaitServer("items entity $playerName inventory.* minecraft:bowl")
        awaitClient("the bowl out of the hotbar") {
            val inv = checkNotNull(it.player).inventory
            inv.getItem(4).isEmpty && (9..35).any { slot -> inv.getItem(slot).`is`(Items.BOWL) }
        }
        check(carried("minecraft:bowl") == 1) { "the bowl was dropped instead of stacked in the inventory" }
        checkNoFurtherClicks(clicks, "AutoBuff Soup")
    }
    disable(ModuleAutoBuff)
}

/** Runs [block] with the count of container clicks the client sends meanwhile. */
private inline fun countingClicks(block: (AtomicInteger) -> Unit) {
    val clicks = AtomicInteger()
    val listener = object : EventListener {}
    listener.on(PacketEvent::class.java) { event ->
        if (event.origin == TransferOrigin.OUTGOING && event.packet is ServerboundContainerClickPacket) {
            clicks.incrementAndGet()
        }
    }.use { block(clicks) }
}

private fun GameTestScope.checkNoFurtherClicks(clicks: AtomicInteger, module: String) {
    val sent = clicks.get()
    ticks(10)
    check(clicks.get() == sent) { "$module sent ${clicks.get() - sent} more clicks after it was done" }
}

private fun GameTestScope.resetInventory() {
    run("kill @e[type=item]")
    run("clear $playerName")
    awaitClient("an empty inventory") { checkNotNull(it.player).inventory.isEmpty }
}

private fun GameTestScope.setSlot(slot: String, item: String, count: Int = 1) {
    run("item replace entity $playerName $slot with $item $count")
}

private fun GameTestScope.useFastConstraints(constraints: ValueGroup) {
    client {
        constraints.inner.first { it.name == "StartDelay" }.setByString("0..0")
        constraints.inner.first { it.name == "ClickDelay" }.setByString("50..50")
        constraints.inner.first { it.name == "CloseDelay" }.setByString("0..0")
    }
}

private fun GameTestScope.awaitCondition(description: String, maxTicks: Int = 120, condition: () -> Boolean) {
    repeat(maxTicks) {
        if (condition()) {
            return
        }
        ticks(1)
    }
    throw AssertionError("Timed out waiting for $description")
}
