/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.util.regex.Pattern
import java.util.stream.IntStream
import kotlin.math.min

/**
 * @author MCModding4K
 */
object ItemUtils : MinecraftInstance()
{
	/**
	 * Allows you to create a item using the item json
	 *
	 * @param  itemArguments
	 * arguments of item
	 * @return               created item
	 * @author               MCModding4K
	 */
	fun createItem(itemArguments: String): IItemStack?
	{
		var itemArgs = itemArguments

		return try
		{
			itemArgs = itemArgs.replace('&', '\u00A7') // Translate Colorcodes

			var item: IItem? = classProvider.createItem()
			var args: List<String>? = null
			var mode = 0
			val modeSize = min(12, itemArgs.length - 2)

			while (mode <= modeSize)
			{
				args = itemArgs.substring(mode).split(Pattern.quote(" "))

				val resourcelocation = classProvider.createResourceLocation(args[0])

				item = functions.getObjectFromItemRegistry(resourcelocation)

				if (item != null) break

				++mode
			}

			if (item == null) return null

			var i = 1

			if ((args ?: return null).size >= 2 && PATTERN.matcher(args[1]).matches()) i = args[1].toInt()

			var j = 0

			if (args.size >= 3 && PATTERN.matcher(args[2]).matches()) j = args[2].toInt()

			val itemstack = classProvider.createItemStack(item, i, j)

			if (args.size >= 4)
			{
				val nbtBuilder = StringBuilder()
				val argsLength = args.size
				for (nbtcount in 3 until argsLength) nbtBuilder.append(" ").append(args[nbtcount])
				itemstack.tagCompound = classProvider.jsonToNBTInstance.getTagFromJson("$nbtBuilder")
			}

			itemstack
		}
		catch (e: Exception)
		{
			// noinspection StringConcatenationArgumentToLogCall
			logger.error("Can't create the item with arguments \"$itemArgs\"", e)
			null
		}
	}

	fun getEnchantment(itemStack: IItemStack?, enchantment: IEnchantment): Int = if (itemStack?.enchantmentTagList == null || itemStack.enchantmentTagList!!.hasNoTags()) 0 else IntStream.range(0, itemStack.enchantmentTagList!!.tagCount()).mapToObj(itemStack.enchantmentTagList!!::getCompoundTagAt).filter { tagCompound: INBTTagCompound -> tagCompound.hasKey("ench") && tagCompound.getShort("ench").toInt() == enchantment.effectId || tagCompound.hasKey("id") && tagCompound.getShort("id").toInt() == enchantment.effectId }.findFirst().map { tagCompound: INBTTagCompound -> tagCompound.getShort("lvl") }.orElseGet(0::toShort).toInt()

	fun getEnchantmentCount(itemStack: IItemStack?): Int = if (itemStack?.enchantmentTagList == null || itemStack.enchantmentTagList!!.hasNoTags()) 0 else IntStream.range(0, itemStack.enchantmentTagList!!.tagCount()).mapToObj(itemStack.enchantmentTagList!!::getCompoundTagAt).filter { tagCompound: INBTTagCompound -> tagCompound.hasKey("ench") || tagCompound.hasKey("id") }.count().toInt()

	fun isStackEmpty(stack: IItemStack?): Boolean = stack == null || classProvider.isItemAir(stack.item)

	private val PATTERN = Pattern.compile("\\d+")
}
