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
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.harness.command
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments

private const val CAPPED_LEVEL = 255

/**
 * The enchant command against the real creative inventory and enchantment registry: the client puts the enchanted
 * item into the hand, and the server takes it.
 */
class EnchantCommandGameTest : LiquidBounceGameTest({
    run("gamemode creative $playerName")
    awaitClient("creative mode") { checkNotNull(it.player).hasInfiniteMaterials() }

    for (command in listOf("all", "all_possible")) {
        checkBulkLevels(command, null, null)
        checkBulkLevels(command, "max", null)
        checkBulkLevels(command, "MAX", null)
        checkBulkLevels(command, "7", 7)
        checkBulkLevels(command, Int.MAX_VALUE.toString(), CAPPED_LEVEL)
    }

    checkSingleLevel(null, null)
    checkSingleLevel("max", null)
    checkSingleLevel("7", 7)
    checkSingleLevel(Int.MAX_VALUE.toString(), CAPPED_LEVEL)
})

private fun GameTestScope.checkBulkLevels(command: String, levelArgument: String?, explicitLevel: Int?) {
    holdSword()
    val input = "enchant $command" + (levelArgument?.let { " $it" } ?: "")
    command(input)

    val sharpness = client { mc ->
        val player = checkNotNull(mc.player)
        val registry = checkNotNull(mc.level).registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        var enchanted = 0
        for (enchantment in registry.asHolderIdMap()) {
            val expected = if (command == "all_possible" && !enchantment.value().canEnchant(player.mainHandItem)) {
                0
            } else {
                enchanted++
                explicitLevel ?: enchantment.value().maxLevel
            }
            val actual = EnchantmentHelper.getItemEnchantmentLevel(enchantment, player.mainHandItem)
            check(actual == expected) {
                "$input: ${enchantment.registeredName} should have level $expected, got $actual"
            }
        }
        check(enchanted > 0) { "$input did not exercise any enchantments" }
        EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.SHARPNESS), player.mainHandItem)
    }
    awaitSharpness(sharpness)
}

private fun GameTestScope.checkSingleLevel(levelArgument: String?, explicitLevel: Int?) {
    holdSword()
    val input = "enchant add sharpness" + (levelArgument?.let { " $it" } ?: "")
    command(input)

    val actual = client { mc ->
        val enchantment = checkNotNull(mc.level).registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.SHARPNESS)
        val expected = explicitLevel ?: enchantment.value().maxLevel
        val actual = EnchantmentHelper.getItemEnchantmentLevel(enchantment, checkNotNull(mc.player).mainHandItem)
        check(actual == expected) { "$input should have level $expected, got $actual" }
        actual
    }
    awaitSharpness(actual)
}

/** Puts a plain diamond sword into the hand, on the server, and waits until the client holds it. */
private fun GameTestScope.holdSword() {
    run("item replace entity $playerName weapon.mainhand with minecraft:diamond_sword")
    awaitClient("a plain sword in the hand") { mc ->
        val item = checkNotNull(mc.player).mainHandItem
        item.`is`(Items.DIAMOND_SWORD) && !item.isEnchanted
    }
}

/** Waits until the server has the sword in the hand enchanted with sharpness of [level]. */
private fun GameTestScope.awaitSharpness(level: Int) = awaitServer(
    "items entity $playerName weapon.mainhand " +
        "minecraft:diamond_sword[minecraft:enchantments~[{enchantments:\"minecraft:sharpness\",levels:$level}]]"
)
