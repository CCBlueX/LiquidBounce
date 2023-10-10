package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import kotlin.math.ceil

object CoroutineArmorComparator: MinecraftInstance() {
	fun getBestArmorSet(stacks: List<ItemStack?>, entityStacksMap: Map<ItemStack, EntityItem>? = null): ArmorSet? {
		val thePlayer = mc.thePlayer ?: return null

		val armorMap = (
			(
				// Consider dropped armor pieces
				if (!entityStacksMap.isNullOrEmpty())
					entityStacksMap.keys.mapNotNull { stack ->
						if (stack.item is ItemArmor) -1 to stack
						else null
					}
				else emptyList()
			) + (
				// Consider currently equipped armor, when searching useful stuff in chests
				// Index is null for equipped armor when searching through a chest to prevent any accidental impossible interactions
				if (thePlayer.openContainer.windowId != 0)
					thePlayer.inventory.armorInventory.mapNotNull { null to (it ?: return@mapNotNull null) }
				else emptyList()
			) + (
				stacks
					.mapIndexedNotNull { index, itemStack ->
						if (itemStack?.item is ItemArmor) index to itemStack
						else null
					}
				)
			)
			.sortedBy { (index, stack) ->
				// Sort items by distance from player, equipped items are always preferred with distance -1
				if (index == -1)
					thePlayer.getDistanceSqToEntity(entityStacksMap?.get(stack) ?: return@sortedBy -1.0)
				else -1.0
			}
			// Prioritise sets that are in lower parts of inventory (not in chest) or equipped, prevents stealing multiple armor duplicates.
			.sortedByDescending { it.first ?: Int.MAX_VALUE }
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
			// Prioritise armor sets that are mostly equipped
			.sortedByDescending { set ->
				set.count {
					// Equipped items are additionally added to stacks map, when searching through a chest.
					// Their slot ids are set to null, in order to make them easily distinguishable from real items in the chest.
					it != null && (it.first == null || it.first in 5..8)
				}
			}

		return armorCombinations.maxByOrNull { it.defenseFactor }
	}
}

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

			// Protection 4 has enchantment protection factor hardcoded to 5, other levels are equal to their epf (see wiki)
			epf += if (protectionLvl == 4) 5 else protectionLvl
		}

		val baseDefense = baseDefensePercentage / 100f

		baseDefense + (1 - baseDefense) * ceil(epf.coerceAtMost(25) * 0.75f) * 0.04f
	}

	override fun iterator() = armorPairs.iterator()

	operator fun contains(stack: ItemStack) = armorPairs.any { it?.second == stack }

	operator fun contains(index: Int) = armorPairs.any { it?.first == index }

	fun indexOf(stack: ItemStack) = armorPairs.find { it?.second == stack }?.first ?: -1

	operator fun get(index: Int) = armorPairs.getOrNull(index)

	val helmet
		get() = armorPairs[0]?.second

	val chestplate
		get() = armorPairs[1]?.second

	val leggings
		get() = armorPairs[2]?.second

	val boots
		get() = armorPairs[3]?.second
}

private val NULL_LIST = listOf<Pair<Int?, ItemStack>?>(null)