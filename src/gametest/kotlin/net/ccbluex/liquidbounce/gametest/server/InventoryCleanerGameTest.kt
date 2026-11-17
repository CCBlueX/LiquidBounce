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
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.restoring
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.concurrent.atomic.AtomicInteger

private const val HEALTH_POTION =
    """minecraft:splash_potion[minecraft:potion_contents={potion:"minecraft:strong_healing"}]"""
private const val WATER_BOTTLE =
    """minecraft:potion[minecraft:potion_contents={potion:"minecraft:water"}]"""
private const val POISON_POTION =
    """minecraft:potion[minecraft:potion_contents={potion:"minecraft:poison"}]"""
private const val PROT_IV_IRON_HELMET =
    """minecraft:iron_helmet[minecraft:enchantments={"minecraft:protection":4}]"""

/**
 * Covers the failure modes seen in SkyWars and BlitzSG inventory recordings:
 * 1. Sorting best gear, merging block stacks, discarding water/bad potions/glass bottles, and `ThrowUnknownItems`.
 * 2. `AutoBuff` `Refill` must not `QUICK_MOVE`-loop on a full 9-slot hotbar or starve `InventoryCleaner`.
 * 3. `AutoArmor` (`RawDefense`) and `InventoryCleaner` must agree so replaced plain diamond armor is thrown.
 * 4. `AutoArmor` `UseItem` right-click and `InventoryCleaner` clicks stay in sync on client and server.
 * 5. Chest interaction sets `isContainerOpenPending` before `ClientboundOpenScreenPacket` arrives.
 * 6. Stray cursor stack is placed back before cleaning without no-op `THROW` spam.
 * 7. Server-reverted clicks lock only the rejected stack while allowing other junk cleanup to continue.
 */
class InventoryCleanerGameTest : SingleplayerGameTest({
    context.restoring(ModuleInventoryCleaner, ModuleAutoArmor, ModuleAutoBuff) {
        run("gamemode survival $playerName")
        awaitClient("survival mode") { checkNotNull(it.player).hasInfiniteMaterials().not() }

        sortsGearMergesBlocksAndCleansJunk()
        autoBuffRefillFullHotbarDoesNotStarveCleaner()
        autoArmorRawDefenseConsistencyCleansInferiorDiamond()
        autoArmorUseItemDoesNotDesyncCleanerSwap()
        chestInteractionSetsContainerPendingBeforeScreenOpens()
        strayCursorItemRecoveredBeforeCleaning()
        serverRevertedItemLocksWithoutBlockingOtherCleanup()
    }
})

private fun GameTestScope.sortsGearMergesBlocksAndCleansJunk() {
    resetInventory()
    useFastConstraints(ModuleInventoryCleaner)
    client {
        ModuleInventoryCleaner.settings.getValue("ThrowUnknownItems").setByString("false")
        ModuleInventoryCleaner.settings.getValue("SlotItem-6").setByString("Potion")
    }

    setSlot("hotbar.0", "minecraft:stone_sword")
    setSlot("inventory.0", "minecraft:iron_sword")
    setSlot("hotbar.8", "minecraft:oak_planks", 20)
    setSlot("inventory.1", "minecraft:oak_planks", 30)
    setSlot("inventory.2", WATER_BOTTLE)
    setSlot("inventory.3", POISON_POTION)
    setSlot("inventory.4", "minecraft:glass_bottle")
    setSlot("inventory.5", HEALTH_POTION)
    setSlot("inventory.6", "minecraft:rail")
    awaitClient("initial junk setup") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.IRON_SWORD) == 1 && inv.countItem(Items.RAIL) == 1
    }

    enable(ModuleInventoryCleaner)
    awaitServer("items entity $playerName hotbar.0 minecraft:iron_sword")
    awaitServer("items entity $playerName hotbar.5 minecraft:splash_potion")
    awaitCondition("junk and inferior gear removed") {
        carried("minecraft:stone_sword") == 0 &&
            carried("minecraft:potion") == 0 &&
            carried("minecraft:glass_bottle") == 0
    }
    check(carried("minecraft:oak_planks") == 50) { "oak planks were not preserved when merging stacks" }
    check(client {
        val inv = checkNotNull(it.player).inventory
        (0 until inv.containerSize).count { slot -> inv.getItem(slot).`is`(Items.OAK_PLANKS) } == 1
    }) { "partial oak planks stacks were not merged into one" }
    check(carried("minecraft:rail") == 1) { "unknown item was thrown while ThrowUnknownItems was disabled" }

    client { ModuleInventoryCleaner.settings.getValue("ThrowUnknownItems").setByString("true") }
    awaitCondition("unknown item thrown when ThrowUnknownItems is enabled") { carried("minecraft:rail") == 0 }
    disable(ModuleInventoryCleaner)
}

private fun GameTestScope.autoBuffRefillFullHotbarDoesNotStarveCleaner() {
    resetInventory()
    useFastConstraints(ModuleInventoryCleaner)
    client {
        ModuleInventoryCleaner.settings.getValue("SlotItem-6").setByString("None")
        (ModuleAutoBuff.settings.getValue("Pot") as ToggleableValueGroup).enabled = true
        (ModuleAutoBuff.settings.getValue("Refill") as ToggleableValueGroup).enabled = true
    }

    setSlot("hotbar.0", "minecraft:iron_sword")
    setSlot("hotbar.1", "minecraft:bow")
    setSlot("hotbar.2", "minecraft:diamond_pickaxe")
    setSlot("hotbar.3", "minecraft:diamond_axe")
    setSlot("hotbar.4", "minecraft:ender_pearl")
    setSlot("hotbar.5", "minecraft:golden_apple")
    setSlot("hotbar.6", "minecraft:cooked_beef", 16)
    setSlot("hotbar.7", "minecraft:oak_planks", 64)
    setSlot("hotbar.8", "minecraft:cobblestone", 64)
    setSlot("inventory.0", HEALTH_POTION)
    setSlot("inventory.1", "minecraft:glass_bottle")
    setSlot("inventory.2", "minecraft:wooden_sword")
    awaitClient("full hotbar with inventory potions and junk") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.GLASS_BOTTLE) == 1 && inv.countItem(Items.WOODEN_SWORD) == 1
    }

    val quickMoveClicks = AtomicInteger()
    val listener = object : EventListener {}
    listener.on(PacketEvent::class.java) { event ->
        val packet = event.packet
        if (event.origin == TransferOrigin.OUTGOING &&
            packet is ServerboundContainerClickPacket &&
            packet.containerInput == ContainerInput.QUICK_MOVE
        ) {
            quickMoveClicks.incrementAndGet()
        }
    }.use {
        enable(ModuleAutoBuff)
        enable(ModuleInventoryCleaner)
        awaitCondition("cleaner threw junk alongside AutoBuff Refill") {
            carried("minecraft:glass_bottle") == 0 && carried("minecraft:wooden_sword") == 0
        }
        ticks(5)
        check(quickMoveClicks.get() == 0) {
            "AutoBuff Refill sent ${quickMoveClicks.get()} QUICK_MOVE clicks with no free hotbar slot"
        }
        check(carried("minecraft:splash_potion") == 1) { "splash potion disappeared during refill check" }
    }

    disable(ModuleInventoryCleaner)
    disable(ModuleAutoBuff)
}

private fun GameTestScope.autoArmorRawDefenseConsistencyCleansInferiorDiamond() {
    resetInventory()
    useFastConstraints(ModuleInventoryCleaner)
    useFastConstraints(ModuleAutoArmor)
    client { ModuleAutoArmor.modes.setByString("RawDefense") }

    setSlot("armor.head", "minecraft:diamond_helmet")
    setSlot("inventory.0", PROT_IV_IRON_HELMET)
    awaitClient("diamond and enchanted iron helmets") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.DIAMOND_HELMET) == 1 && inv.countItem(Items.IRON_HELMET) == 1
    }

    enable(ModuleAutoArmor)
    enable(ModuleInventoryCleaner)
    awaitServer("items entity $playerName armor.head minecraft:iron_helmet")
    awaitCondition("inferior diamond helmet cleaned after RawDefense swap") {
        carried("minecraft:diamond_helmet") == 0
    }

    disable(ModuleInventoryCleaner)
    disable(ModuleAutoArmor)
}

private fun GameTestScope.autoArmorUseItemDoesNotDesyncCleanerSwap() {
    resetInventory()
    useFastConstraints(ModuleInventoryCleaner)
    useFastConstraints(ModuleAutoArmor)
    client {
        ModuleAutoArmor.modes.setByString("Smart")
        (ModuleAutoArmor.settings.getValue("Hotbar") as ToggleableValueGroup).enabled = true
    }

    setSlot("hotbar.0", "minecraft:stone_sword")
    setSlot("hotbar.8", "minecraft:iron_chestplate")
    setSlot("inventory.0", "minecraft:diamond_sword")
    awaitClient("hotbar chestplate and upgrade sword") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.IRON_CHESTPLATE) == 1 && inv.countItem(Items.DIAMOND_SWORD) == 1
    }

    enable(ModuleAutoArmor)
    enable(ModuleInventoryCleaner)
    awaitServer("items entity $playerName armor.chest minecraft:iron_chestplate")
    awaitServer("items entity $playerName hotbar.0 minecraft:diamond_sword")
    awaitCondition("stone sword discarded after UseItem equip") { carried("minecraft:stone_sword") == 0 }
    check(client { it.player?.inventory?.countItem(Items.STONE_SWORD) == 0 }) {
        "client and server disagreed on stone sword removal"
    }

    disable(ModuleInventoryCleaner)
    disable(ModuleAutoArmor)
}

private fun GameTestScope.chestInteractionSetsContainerPendingBeforeScreenOpens() {
    resetInventory()
    useFastConstraints(ModuleInventoryCleaner)
    val chestPos = origin.north(2)
    setBlock(chestPos, "minecraft:chest")
    awaitServer("block ${chestPos.x} ${chestPos.y} ${chestPos.z} minecraft:chest")

    setSlot("inventory.0", "minecraft:glass_bottle")
    awaitClient("glass bottle before chest open") {
        checkNotNull(it.player).inventory.countItem(Items.GLASS_BOTTLE) == 1
    }

    enable(ModuleInventoryCleaner)
    val pendingObserved = client { mc ->
        val player = checkNotNull(mc.player)
        val hit = BlockHitResult(Vec3.atCenterOf(chestPos), Direction.UP, chestPos, false)
        checkNotNull(mc.gameMode).useItemOn(player, InteractionHand.MAIN_HAND, hit)
        InventoryManager.isContainerOpenPending
    }
    check(pendingObserved) { "isContainerOpenPending was not set immediately when interacting with a chest" }

    awaitClient("chest screen open") { it.gui.screen() != null }
    ticks(5)
    check(carried("minecraft:glass_bottle") == 1) {
        "InventoryCleaner acted while chest open was pending or chest screen was open"
    }
    disable(ModuleInventoryCleaner)
    client { checkNotNull(it.player).closeContainer() }
    awaitClient("chest screen closed") { it.gui.screen() == null }
    ticks(2)
    setBlock(chestPos, "minecraft:air")
}

private fun GameTestScope.strayCursorItemRecoveredBeforeCleaning() {
    resetInventory()
    useFastConstraints(ModuleInventoryCleaner)

    setSlot("inventory.0", "minecraft:golden_apple")
    setSlot("inventory.1", "minecraft:glass_bottle")
    awaitClient("cursor test setup") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.GOLDEN_APPLE) == 1 && inv.countItem(Items.GLASS_BOTTLE) == 1
    }

    client { mc ->
        val player = checkNotNull(mc.player)
        checkNotNull(mc.gameMode).handleContainerInput(0, 9, 0, ContainerInput.PICKUP, player)
        check(!player.containerMenu.carried.isEmpty) { "expected golden apple on cursor before cleaning" }
    }

    val throwClicks = AtomicInteger()
    val listener = object : EventListener {}
    listener.on(PacketEvent::class.java) { event ->
        val packet = event.packet
        if (event.origin == TransferOrigin.OUTGOING &&
            packet is ServerboundContainerClickPacket &&
            packet.containerInput == ContainerInput.THROW
        ) {
            throwClicks.incrementAndGet()
        }
    }.use {
        enable(ModuleInventoryCleaner)
        awaitCondition("cursor recovered and glass bottle thrown") {
            carried("minecraft:glass_bottle") == 0 &&
                client { checkNotNull(it.player).containerMenu.carried.isEmpty }
        }
        check(throwClicks.get() == 1) {
            "expected single THROW after cursor recovery, sent ${throwClicks.get()}"
        }
        check(carried("minecraft:golden_apple") == 1) {
            "stray golden apple on cursor was lost during recovery"
        }
    }

    disable(ModuleInventoryCleaner)
}

private fun GameTestScope.serverRevertedItemLocksWithoutBlockingOtherCleanup() {
    resetInventory()
    useFastConstraints(ModuleInventoryCleaner)
    client { ModuleInventoryCleaner.settings.getValue("ThrowUnknownItems").setByString("true") }

    setSlot("hotbar.0", "minecraft:diamond_sword")
    setSlot("hotbar.4", "minecraft:compass")
    setSlot("inventory.0", "minecraft:glass_bottle")
    setSlot("inventory.1", "minecraft:wooden_sword")
    awaitClient("compass and junk") {
        val inv = checkNotNull(it.player).inventory
        inv.countItem(Items.COMPASS) == 1 && inv.countItem(Items.GLASS_BOTTLE) == 1 &&
            inv.countItem(Items.WOODEN_SWORD) == 1
    }

    val compassClicks = AtomicInteger()
    val listener = object : EventListener {}
    listener.on(PacketEvent::class.java) { event ->
        val packet = event.packet
        if (event.origin == TransferOrigin.OUTGOING &&
            packet is ServerboundContainerClickPacket &&
            packet.slotNum.toInt() == 40
        ) {
            compassClicks.incrementAndGet()
        }
    }.use {
        enable(ModuleInventoryCleaner)
        var restoredClicks = 0
        awaitCondition("compass locked and junk thrown", maxTicks = 200) {
            val seenClicks = compassClicks.get()
            if (seenClicks > restoredClicks) {
                restoredClicks = seenClicks
                ticks(1)
                run("kill @e[type=item]")
                setSlot("hotbar.4", "minecraft:compass")
            }
            client { mc ->
                val player = checkNotNull(mc.player)
                InventoryManager.isServerLocked(ItemStack(Items.COMPASS)) &&
                    player.inventory.countItem(Items.GLASS_BOTTLE) == 0 &&
                    player.inventory.countItem(Items.WOODEN_SWORD) == 0
            }
        }

        val clicksAtLock = compassClicks.get()
        ticks(10)
        check(compassClicks.get() == clicksAtLock) {
            "InventoryCleaner kept clicking locked compass (${compassClicks.get()} vs $clicksAtLock)"
        }
        check(carried("minecraft:glass_bottle") == 0 && carried("minecraft:wooden_sword") == 0) {
            "server-locked compass prevented cleaning other junk"
        }
        check(carried("minecraft:diamond_sword") == 1 && carried("minecraft:compass") == 1) {
            "diamond sword or locked compass went missing"
        }
    }

    disable(ModuleInventoryCleaner)
}

private fun GameTestScope.resetInventory() {
    run("kill @e[type=item]")
    run("clear $playerName")
    run("kill @e[type=item]")
    repeat(200) {
        if (client { checkNotNull(it.player).inventory.isEmpty }) {
            return
        }
        ticks(1)
    }
    val remaining = client { mc ->
        val inv = checkNotNull(mc.player).inventory
        (0 until inv.containerSize).mapNotNull { slot ->
            inv.getItem(slot).takeUnless { it.isEmpty }?.let { "$slot=${it.item}:${it.count}" }
        }
    }
    val serverBottles = carried("minecraft:glass_bottle")
    val menuInfo = client { mc ->
        val p = checkNotNull(mc.player)
        "screen=${mc.gui.screen()} menu=${p.containerMenu.containerId}"
    }
    throw AssertionError(
        "Inventory did not clear after /clear; client=$remaining serverBottles=$serverBottles $menuInfo"
    )
}

private fun GameTestScope.setSlot(slot: String, item: String, count: Int = 1) {
    run("item replace entity $playerName $slot with $item $count")
}

private fun GameTestScope.useFastConstraints(module: ClientModule) {
    client {
        val constraints = module.settings.getValue("Constraints") as ValueGroup
        constraints.inner.first { it.name == "StartDelay" }.setByString("0..0")
        constraints.inner.first { it.name == "ClickDelay" }.setByString("1..1")
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
