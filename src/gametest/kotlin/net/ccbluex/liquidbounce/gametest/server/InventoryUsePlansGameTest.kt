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
import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.teleport
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleVomit
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBookBot
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleElytraSwap
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.ElytraFlyModeFirework
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * Modules that use an item through an inventory plan:
 * 1. `ElytraSwap` swaps chestplate and elytra once per enabling, also when the server changes the inventory midway.
 * 2. `ElytraFly` `Firework` uses a rocket from the inventory and swaps it back.
 * 3. `Vomit` throws everything out.
 * 4. `BookBot` takes a book from the inventory into the hand and signs it.
 */
class InventoryUsePlansGameTest : SingleplayerGameTest(usePlans)

/** The same on the game test server, where the run's anticheat watches the clicks. */
class InventoryUsePlansServerGameTest : ServerGameTest(usePlans)

private val usePlans: GameTestScope.() -> Unit = {
    context.restoring(ModuleElytraSwap, ModuleElytraFly, ModuleVomit, ModuleBookBot) {
        run("gamemode survival $playerName")
        awaitClient("survival mode") { checkNotNull(it.player).hasInfiniteMaterials().not() }

        elytraSwapSwapsOncePerEnabling()
        fireworkIsUsedFromInventory()
        vomitEmptiesInventory()
        bookBotSignsBookFromInventory()
    }
}

private fun GameTestScope.elytraSwapSwapsOncePerEnabling() {
    resetInventory()
    useFastConstraints(constraintsOf(ModuleElytraSwap))
    setSlot("armor.chest", "minecraft:diamond_chestplate")
    setSlot("inventory.0", "minecraft:elytra")
    awaitClient("chestplate and elytra") { it.wears(Items.DIAMOND_CHESTPLATE) && it.holdsInFirstSlot(Items.ELYTRA) }

    enable(ModuleElytraSwap)
    awaitServer("items entity $playerName armor.chest minecraft:elytra")
    awaitServer("items entity $playerName inventory.0 minecraft:diamond_chestplate")
    awaitClient("the elytra on and ElytraSwap off") {
        it.wears(Items.ELYTRA) && it.holdsInFirstSlot(Items.DIAMOND_CHESTPLATE) && !ModuleElytraSwap.enabled
    }

    enable(ModuleElytraSwap)
    awaitServer("items entity $playerName armor.chest minecraft:diamond_chestplate")
    awaitServer("items entity $playerName inventory.0 minecraft:elytra")
    awaitClient("the chestplate back on and ElytraSwap off") {
        it.wears(Items.DIAMOND_CHESTPLATE) && it.holdsInFirstSlot(Items.ELYTRA) && !ModuleElytraSwap.enabled
    }

    // Three clicks 200 ms apart, so the stick arrives between them
    useFastConstraints(constraintsOf(ModuleElytraSwap), clickDelay = 200)
    enable(ModuleElytraSwap)
    setSlot("inventory.5", "minecraft:stick")
    awaitServer("items entity $playerName armor.chest minecraft:elytra")
    awaitClient("ElytraSwap off after the interrupted swap") { !ModuleElytraSwap.enabled }
    awaitCondition("the chestplate back in the inventory") {
        carried("minecraft:diamond_chestplate") == 1 &&
            client { checkNotNull(it.player).inventory.countItem(Items.DIAMOND_CHESTPLATE) == 1 }
    }

    ticks(40)
    check(client { it.wears(Items.ELYTRA) }) { "ElytraSwap swapped back after the server changed the inventory" }
    awaitServer("items entity $playerName armor.chest minecraft:elytra")
    check(carried("minecraft:diamond_chestplate") == 1) { "the server lost the chestplate during the swap" }
    check(carried("minecraft:stick") == 1) { "the stick the server gave went missing" }
    check(client { !ModuleElytraSwap.enabled }) { "ElytraSwap turned itself on again" }
}

private fun GameTestScope.fireworkIsUsedFromInventory() {
    resetInventory()
    val considerInventory =
        ElytraFlyModeFirework.inner.first { it.name == "ConsiderInventory" } as ToggleableValueGroup
    client {
        ModuleElytraFly.modes.setByString("Firework")
        considerInventory.enabled = true
        // One rocket for the whole flight, so the counts below are of a finished chain
        ElytraFlyModeFirework.inner.first { it.name == "Cooldown" }.setByString("300..300")
    }
    useFastConstraints(considerInventory.inner.first { it.name == "Constraints" } as ValueGroup)
    setSlot("armor.chest", "minecraft:elytra")
    setSlot("weapon.mainhand", "minecraft:diamond_sword")
    setSlot("inventory.0", "minecraft:firework_rocket", 8)
    awaitClient("elytra, sword and rockets") { mc ->
        mc.wears(Items.ELYTRA) && mc.holdsInFirstSlot(Items.FIREWORK_ROCKET) &&
            checkNotNull(mc.player).mainHandItem.`is`(Items.DIAMOND_SWORD)
    }
    // Landing after the flight must not hurt
    run("effect give $playerName minecraft:resistance 120 4 true")

    try {
        teleport(origin.above(80))
        awaitClient("the fall") { checkNotNull(it.player).deltaMovement.y < -0.5 }
        input.holdKey { it.keyJump }
        ticks(2)
        input.releaseKey { it.keyJump }
        awaitClient("gliding") { checkNotNull(it.player).isFallFlying }

        enable(ModuleElytraFly)
        awaitCondition("a rocket used and the rest swapped back") {
            client { mc ->
                val player = checkNotNull(mc.player)
                player.inventory.countItem(Items.FIREWORK_ROCKET) == 7 &&
                    mc.holdsInFirstSlot(Items.FIREWORK_ROCKET) &&
                    player.offhandItem.isEmpty && player.mainHandItem.`is`(Items.DIAMOND_SWORD)
            }
        }
        disable(ModuleElytraFly)
        // The client is a tick ahead of the server with what it clicked
        awaitServer("items entity $playerName inventory.0 minecraft:firework_rocket")
        awaitServer("items entity $playerName weapon.mainhand minecraft:diamond_sword")
        val rockets = carried("minecraft:firework_rocket")
        check(rockets == 7) { "the server has $rockets rockets in the inventory instead of 7" }
    } finally {
        disable(ModuleElytraFly)
        run("item replace entity $playerName armor.chest with minecraft:air")
        awaitClient("gliding to end") { !checkNotNull(it.player).isFallFlying }
        travel(origin)
        run("effect clear $playerName")
    }
}

private fun GameTestScope.vomitEmptiesInventory() {
    resetInventory()
    useFastConstraints(constraintsOf(ModuleVomit))
    setSlot("hotbar.0", "minecraft:cobblestone", 32)
    setSlot("hotbar.4", "minecraft:stick", 3)
    setSlot("inventory.0", "minecraft:oak_planks", 16)
    setSlot("inventory.10", "minecraft:iron_ingot", 5)
    awaitClient("four stacks") { mc ->
        val inventory = checkNotNull(mc.player).inventory
        inventory.countItem(Items.COBBLESTONE) == 32 && inventory.countItem(Items.IRON_INGOT) == 5
    }

    enable(ModuleVomit)
    awaitCondition("everything thrown") { client { checkNotNull(it.player).inventory.isEmpty } }
    // What was thrown can be picked up again after two seconds
    disable(ModuleVomit)
    run("execute at $playerName run kill @e[type=item,distance=..16]")

    ticks(10)
    val left = listOf("minecraft:cobblestone", "minecraft:stick", "minecraft:oak_planks", "minecraft:iron_ingot")
        .filter { carried(it) > 0 }
    check(left.isEmpty()) { "the server still has $left in the inventory after Vomit" }
    check(client { checkNotNull(it.player).inventory.isEmpty }) { "items came back after Vomit threw them" }
}

private fun GameTestScope.bookBotSignsBookFromInventory() {
    resetInventory()
    useFastConstraints(constraintsOf(ModuleBookBot))
    client {
        // Paper kicks for a second edit within a second
        ModuleBookBot.settings.getValue("Delay").setByString("2")
        val mode = (ModuleBookBot.settings.getValue("Mode") as ModeValueGroup<*>).activeMode
        mode.inner.first { it.name == "Pages" }.setByString("2")
        mode.inner.first { it.name == "AsciiOnly" }.setByString("true")
    }
    setSlot("weapon.mainhand", "minecraft:stick")
    setSlot("inventory.0", "minecraft:writable_book")
    awaitClient("stick in hand and book in the inventory") { mc ->
        mc.holdsInFirstSlot(Items.WRITABLE_BOOK) && checkNotNull(mc.player).mainHandItem.`is`(Items.STICK)
    }

    if (Anticheat.GRIM in anticheats) {
        // Grim takes no edit of a book the player has not opened, so only the swap can arrive
        expectViolation("an edit of a book that is not open") {
            enable(ModuleBookBot)
            awaitServer("items entity $playerName weapon.mainhand minecraft:writable_book")
            awaitServer("items entity $playerName inventory.0 minecraft:stick")
            // The first edit comes after the delay
            ticks(60)
            disable(ModuleBookBot)
        }
        return
    }

    enable(ModuleBookBot)
    awaitServer("items entity $playerName weapon.mainhand minecraft:written_book")
    awaitClient("the signed book in hand and BookBot off") { mc ->
        checkNotNull(mc.player).mainHandItem.`is`(Items.WRITTEN_BOOK) && !ModuleBookBot.enabled
    }
    check(client { it.holdsInFirstSlot(Items.STICK) }) { "the stick did not go where the book was" }
    check(carried("minecraft:writable_book") == 0 && carried("minecraft:written_book") == 1) {
        "the server does not have exactly the one signed book"
    }
    awaitServer("items entity $playerName inventory.0 minecraft:stick")
}

private fun Minecraft.wears(item: Item) =
    checkNotNull(player).getItemBySlot(EquipmentSlot.CHEST).`is`(item)

/** Whether `inventory.0` holds [item]. */
private fun Minecraft.holdsInFirstSlot(item: Item) =
    checkNotNull(player).inventory.getItem(9).`is`(item)

private fun constraintsOf(module: ClientModule) = module.settings.getValue("Constraints") as ValueGroup

private fun GameTestScope.useFastConstraints(constraints: ValueGroup, clickDelay: Int = 50) {
    client {
        constraints.inner.first { it.name == "StartDelay" }.setByString("0..0")
        constraints.inner.first { it.name == "ClickDelay" }.setByString("$clickDelay..$clickDelay")
        constraints.inner.first { it.name == "CloseDelay" }.setByString("0..0")
    }
}

private fun GameTestScope.resetInventory() {
    run("execute at $playerName run kill @e[type=item,distance=..16]")
    run("clear $playerName")
    run("execute at $playerName run kill @e[type=item,distance=..16]")
    awaitClient("an empty inventory") { mc ->
        val player = checkNotNull(mc.player)
        player.inventory.isEmpty && player.containerMenu.carried.isEmpty
    }
}

private fun GameTestScope.setSlot(slot: String, item: String, count: Int = 1) {
    run("item replace entity $playerName $slot with $item $count")
}

private fun GameTestScope.awaitCondition(description: String, maxTicks: Int = 200, condition: () -> Boolean) {
    repeat(maxTicks) {
        if (condition()) {
            return
        }
        ticks(1)
    }
    throw AssertionError("Timed out waiting for $description")
}
