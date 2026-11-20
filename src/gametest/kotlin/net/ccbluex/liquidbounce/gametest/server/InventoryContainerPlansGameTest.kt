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
import net.ccbluex.gametest.harness.GameTestScope.Companion.DEFAULT_TIMEOUT
import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleChestCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.autodeposit.ModuleAutoDeposit
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.configure
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.restoring
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.concurrent.atomic.AtomicInteger

private const val JUNK_CHEST =
    """minecraft:chest{Items:[{Slot:0b,id:"minecraft:glass_bottle",count:1},""" +
        """{Slot:5b,id:"minecraft:rotten_flesh",count:8},{Slot:13b,id:"minecraft:glass_bottle",count:4},""" +
        """{Slot:20b,id:"minecraft:diamond",count:3}]}"""

/**
 * The container modules on inventory plans, each with one click per stack and none after the work is done:
 * 1. `AutoDeposit` moves every stack of its filter into an opened chest and closes it.
 * 2. `ChestCleaner` throws every stack of its filter out of an opened chest and closes it.
 */
class InventoryContainerPlansGameTest : SingleplayerGameTest(containerPlans)

/** The same on the game test server, where the run's anticheat watches the clicks. */
class InventoryContainerPlansServerGameTest : ServerGameTest(containerPlans)

private val containerPlans: GameTestScope.() -> Unit = {
    context.restoring(ModuleAutoDeposit, ModuleChestCleaner) {
        run("gamemode survival $playerName")
        awaitClient("survival mode") { checkNotNull(it.player).hasInfiniteMaterials().not() }

        depositsMatchingStacks()
        cleansSelectedStacks()
    }
}

private fun GameTestScope.depositsMatchingStacks() {
    resetInventory()
    useFastConstraints(ModuleAutoDeposit)
    configure(
        ModuleAutoDeposit,
        "Filter" to Filter.WHITELIST,
        "Items" to itemSortedSetOf(Items.COBBLESTONE, Items.OAK_PLANKS),
        "AutoClose" to true,
    )
    val chestPos = origin.north(2)
    setBlock(chestPos, "minecraft:chest")
    awaitServer("block ${chestPos.x} ${chestPos.y} ${chestPos.z} minecraft:chest")

    setSlot("hotbar.0", "minecraft:cobblestone", 64)
    setSlot("hotbar.1", "minecraft:iron_sword")
    setSlot("inventory.0", "minecraft:cobblestone", 32)
    setSlot("inventory.5", "minecraft:oak_planks", 16)
    awaitClient("stacks to deposit and a sword") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.COBBLESTONE) == 96 && inv.countItem(Items.OAK_PLANKS) == 16 &&
            inv.countItem(Items.IRON_SWORD) == 1
    }

    countingClicks(ContainerInput.QUICK_MOVE) { clicks ->
        enable(ModuleAutoDeposit)
        openChest(chestPos)
        awaitCondition("the matching stacks in the chest") {
            stored(chestPos, "minecraft:cobblestone") == 96 && stored(chestPos, "minecraft:oak_planks") == 16
        }
        awaitClient("AutoDeposit to close the chest") { it.gui.screen() == null }
        ticks(10)
        check(clicks.get() == 3) { "AutoDeposit sent ${clicks.get()} QUICK_MOVE clicks for 3 matching stacks" }
    }
    check(carried("minecraft:cobblestone") == 0 && carried("minecraft:oak_planks") == 0) {
        "the server still has deposited items on the player"
    }
    check(carried("minecraft:iron_sword") == 1) { "AutoDeposit moved an item its filter does not select" }
    check(stored(chestPos, "minecraft:iron_sword") == 0) { "the sword ended up in the chest" }
    check(client {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.COBBLESTONE) == 0 && inv.countItem(Items.OAK_PLANKS) == 0 &&
            inv.getItem(1).`is`(Items.IRON_SWORD)
    }) { "the client inventory does not show the deposit: matching stacks left or the sword moved" }

    disable(ModuleAutoDeposit)
    setBlock(chestPos, "minecraft:air")
}

private fun GameTestScope.cleansSelectedStacks() {
    resetInventory()
    useFastConstraints(ModuleChestCleaner)
    configure(
        ModuleChestCleaner,
        "Filter" to Filter.WHITELIST,
        "Items" to itemSortedSetOf(Items.GLASS_BOTTLE, Items.ROTTEN_FLESH),
        "AutoClose" to false,
    )
    val chestPos = origin.south(2)
    setBlock(chestPos, JUNK_CHEST)
    awaitServer("items block ${chestPos.x} ${chestPos.y} ${chestPos.z} container.20 minecraft:diamond")

    countingClicks(ContainerInput.THROW) { clicks ->
        enable(ModuleChestCleaner)
        openChest(chestPos)
        awaitCondition("the selected stacks thrown out of the chest") {
            // Thrown items lie at the player's feet and would be picked up again after two seconds
            run("execute at $playerName run kill @e[type=item,distance=..8]")
            chestItems(Items.GLASS_BOTTLE) == 0 && chestItems(Items.ROTTEN_FLESH) == 0
        }
        ticks(10)
        run("execute at $playerName run kill @e[type=item,distance=..8]")
        check(clicks.get() == 3) { "ChestCleaner sent ${clicks.get()} THROW clicks for 3 selected stacks" }
        check(chestItems(Items.DIAMOND) == 3) { "ChestCleaner threw an item its filter does not select" }
        check(client { it.gui.screen() is AbstractContainerScreen<*> }) {
            "ChestCleaner closed the chest while AutoClose was disabled"
        }
        check(
            stored(chestPos, "minecraft:glass_bottle") == 0 && stored(chestPos, "minecraft:rotten_flesh") == 0
        ) { "the server still has thrown items in the chest" }
        check(stored(chestPos, "minecraft:diamond") == 3) { "the server lost the diamonds of the chest" }

        configure(ModuleChestCleaner, "AutoClose" to true)
        awaitClient("ChestCleaner to close the chest") { it.gui.screen() == null }
        check(clicks.get() == 3) { "ChestCleaner threw again before closing the chest" }
    }
    check(carried("minecraft:diamond") == 0) { "the diamonds of the chest ended up on the player" }

    disable(ModuleChestCleaner)
    setBlock(chestPos, "minecraft:air")
    run("execute at $playerName run kill @e[type=item,distance=..8]")
}

private fun GameTestScope.openChest(pos: BlockPos) {
    client { mc ->
        val hit = BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        checkNotNull(mc.gameMode).useItemOn(checkNotNull(mc.player), InteractionHand.MAIN_HAND, hit)
    }
    awaitClient("chest screen open") { it.gui.screen() is AbstractContainerScreen<*> }
}

/** How many items of [item] the open container holds on the client, without the player's own slots. */
private fun GameTestScope.chestItems(item: Item): Int = client { mc ->
    val player = checkNotNull(mc.player)
    player.containerMenu.slots
        .filter { it.container !== player.inventory && it.item.`is`(item) }
        .sumOf { it.item.count }
}

/** How many items of [item] the container at [pos] holds as the server has it. */
private fun GameTestScope.stored(pos: BlockPos, item: String): Int {
    val id = run("execute if items block ${pos.x} ${pos.y} ${pos.z} container.* $item")
    repeat(DEFAULT_TIMEOUT) {
        server.result(id)?.let { return it }
        ticks(1)
    }
    throw AssertionError("The server never counted $item in the chest, see ${server.log}")
}

/** Runs [block] with the number of outgoing [input] clicks sent so far. */
private fun countingClicks(input: ContainerInput, block: (AtomicInteger) -> Unit) {
    val clicks = AtomicInteger()
    val listener = object : EventListener {}
    listener.on(PacketEvent::class.java) { event ->
        val packet = event.packet
        if (event.origin == TransferOrigin.OUTGOING &&
            packet is ServerboundContainerClickPacket &&
            packet.containerInput == input
        ) {
            clicks.incrementAndGet()
        }
    }.use {
        block(clicks)
    }
}

private fun GameTestScope.resetInventory() {
    run("clear $playerName")
    run("execute at $playerName run kill @e[type=item,distance=..8]")
    awaitClient("an empty inventory") { checkNotNull(it.player).inventory.isEmpty }
}

private fun GameTestScope.setSlot(slot: String, item: String, count: Int = 1) {
    run("item replace entity $playerName $slot with $item $count")
}

private fun GameTestScope.useFastConstraints(module: ClientModule) {
    client {
        val constraints = module.settings.getValue("Constraints") as ValueGroup
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
