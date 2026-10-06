/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of the License, or
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

import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.registries.Registries
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments

/**
 * Exercises the enchant command against the real creative inventory and enchantment registry.
 */
class EnchantCommandGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode creative @a")
            context.waitFor({ client -> client.player?.hasInfiniteMaterials() == true }, 200)

            for (command in listOf("all", "all_possible")) {
                assertBulkLevels(context, command, null, null)
                assertBulkLevels(context, command, "max", null)
                assertBulkLevels(context, command, "MAX", null)
                assertBulkLevels(context, command, "7", 7)
                assertBulkLevels(context, command, Int.MAX_VALUE.toString(), 255)
            }

            assertSingleLevel(context, null, null)
            assertSingleLevel(context, "max", null)
            assertSingleLevel(context, "7", 7)
            assertSingleLevel(context, Int.MAX_VALUE.toString(), 255)
        }
    }

    private fun assertBulkLevels(
        context: ClientGameTestContext,
        command: String,
        levelArgument: String?,
        explicitLevel: Int?,
    ) {
        context.onClient {
            val player = checkNotNull(mc.player)
            val item = ItemStack(Items.DIAMOND_SWORD)
            player.setItemInHand(InteractionHand.MAIN_HAND, item)
            val input = "enchant $command" + (levelArgument?.let { " $it" } ?: "")

            CommandManager.execute(input)

            val registry = checkNotNull(mc.level).registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            var enchanted = 0
            for (enchantment in registry.asHolderIdMap()) {
                val expected = if (command == "all_possible" && !enchantment.value().canEnchant(item)) {
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
        }
    }

    private fun assertSingleLevel(context: ClientGameTestContext, levelArgument: String?, explicitLevel: Int?) {
        context.onClient {
            val player = checkNotNull(mc.player)
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.DIAMOND_SWORD))
            val input = "enchant add sharpness" + (levelArgument?.let { " $it" } ?: "")

            CommandManager.execute(input)

            val enchantment = checkNotNull(mc.level).registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS)
            val expected = explicitLevel ?: enchantment.value().maxLevel
            val actual = EnchantmentHelper.getItemEnchantmentLevel(enchantment, player.mainHandItem)
            check(actual == expected) { "$input should have level $expected, got $actual" }
        }
    }

}
