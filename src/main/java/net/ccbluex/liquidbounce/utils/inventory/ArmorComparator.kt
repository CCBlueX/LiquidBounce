/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack

object ArmorComparator: MinecraftInstance() {
	fun getBestArmorSet(stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): ArmorSet? {
		val thePlayer = mc.thePlayer ?: return null

		// Consider armor pieces dropped on ground
		// Their indices are always -1
		val droppedStacks = entityStacksMap?.keys.indexedArmorStacks { -1 }

		// Consider currently equipped armor, when searching useful stuff in chests
		// Their indices are always null to prevent any accidental impossible interactions when searching through chests
		val equippedArmorWhenInChest =
			if (thePlayer.openContainer.windowId != 0)
				// Filter out any non armor items player could be equipped (skull / pumpkin)
				thePlayer.inventory.armorInventory.toList().indexedArmorStacks { null }
			else emptyList()

		val inventoryStacks = stacks.indexedArmorStacks()

		val armorMap =
			(droppedStacks + equippedArmorWhenInChest + inventoryStacks)
				.asSequence()
				.sortedBy { (index, stack) ->
					// Sort items by distance from player, equipped items are always preferred with distance -1
					if (index == -1)
						thePlayer.getDistanceSqToEntity(entityStacksMap?.get(stack) ?: return@sortedBy -1.0)
					else -1.0
				}
				// Prioritise sets that are in lower parts of inventory (not in chest) or equipped, prevents stealing multiple armor duplicates.
				.sortedByDescending {
					if (it.second in thePlayer.inventory.armorInventory) Int.MAX_VALUE
					else it.first ?: Int.MAX_VALUE
				}
				// Prioritise sets with more durability, enchantments
				.sortedByDescending { it.second.totalDurability }
				.sortedByDescending { it.second.enchantmentCount }
				.sortedByDescending { it.second.enchantmentSum }
				.groupBy { (it.second.item as ItemArmor).armorType }

		val helmets = armorMap[0] ?: NULL_LIST
		val chestplates = armorMap[1] ?: NULL_LIST
		val leggings = armorMap[2] ?: NULL_LIST
		val boots = armorMap[3] ?: NULL_LIST

		val armorCombinations =
			helmets.flatMap { helmet ->
				chestplates.flatMap { chestplate ->
					leggings.flatMap { leggings ->
						boots.map { boots ->
							ArmorSet(helmet, chestplate, leggings, boots)
						}
					}
				}
			}

		return armorCombinations.maxByOrNull { it.defenseFactor }
	}
}

/**
 * This function takes an iterable of ItemStacks and an optional index callback function,
 * and returns a list of pairs. Each pair consists of an index and an ItemStack.
 *
 * @param indexCallback A function that takes an integer as input and returns an integer.
 *                      This function is used to manipulate the index of each ItemStack in the iterable.
 *                      By default, it returns the same index.
 *
 * @return A list of pairs. Each pair consists of an index (possibly manipulated by the indexCallback function)
 *         and an ItemStack. Only ItemStacks where the item is an instance of ItemArmor are included in the list.
 *         If the iterable is null, an empty list is returned.
 */
private fun Iterable<ItemStack?>?.indexedArmorStacks(indexCallback: (Int) -> Int? = { it }): List<Pair<Int?, ItemStack>> =
	this?.mapIndexedNotNull { index, stack ->
		if (stack?.item is ItemArmor) indexCallback(index) to stack
		else null
	} ?: emptyList()

class ArmorSet(private vararg val armorPairs: Pair<Int?, ItemStack>?) : Iterable<Pair<Int?, ItemStack>?> {
	/**
	 * 1.4.6 - 1.8.9 Armor calculations
	 * https://minecraft.fandom.com/wiki/Armor?oldid=927013#Enchantments
	 *
	 * @return Average defense of the whole armor set.
	 */
	val defenseFactor by lazy {
		var baseDefensePercentage = 0
		var epf = 0

		forEach { pair ->
			val stack = pair?.second ?: return@forEach
			val item = stack.item as ItemArmor
			baseDefensePercentage += item.armorMaterial.getDamageReductionAmount(item.armorType) * 4

			val protectionLvl = stack.getEnchantmentLevel(Enchantment.protection)

			// Calculate epf based on protection level
			if (protectionLvl > 0)
				epf += ((6 + protectionLvl * protectionLvl) * 0.75f / 3).toInt()
		}

		val baseDefense = baseDefensePercentage / 100f

		// Not ceiling epf up to simulate the fact that 0.75f is actually random number between 0.5 and 1
		// By ceiling up, you for example get that 3x protection 1 is same as 4x protection 1, even tho 4x protection 1 has better overall average defense
		// More details: https://www.guilded.gg/CCBlueX/groups/1dgpg8Jz/channels/034be45e-1b72-4d5a-bee7-d6ba52ba1657/chat?messageId=c0d88f1e-5ad6-48f3-8acb-d5ab7611164b
		baseDefense + (1 - baseDefense) * epf.coerceAtMost(25) * 0.75f * 0.04f
	}

	override fun iterator() = armorPairs.iterator()

	operator fun contains(stack: ItemStack) = armorPairs.any { it?.second == stack }

	operator fun contains(index: Int) = armorPairs.any { it?.first == index }

	fun indexOf(stack: ItemStack) = armorPairs.find { it?.second == stack }?.first ?: -1

	operator fun get(index: Int) = armorPairs.getOrNull(index)
}

operator fun ArmorSet?.contains(stack: ItemStack) = this?.contains(stack) ?: true

private val NULL_LIST = listOf<Pair<Int?, ItemStack>?>(null)