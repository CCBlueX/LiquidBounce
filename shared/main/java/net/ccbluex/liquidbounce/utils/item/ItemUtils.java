/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment;
import net.ccbluex.liquidbounce.api.minecraft.item.IItem;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;

import org.jetbrains.annotations.Contract;

/**
 * @author MCModding4K
 */
public final class ItemUtils extends MinecraftInstance
{

	/**
	 * Allows you to create a item using the item json
	 *
	 * @param  itemArguments
	 *                       arguments of item
	 * @return               created item
	 * @author               MCModding4K
	 */
	public static IItemStack createItem(String itemArguments)
	{
		try
		{
			itemArguments = itemArguments.replace('&', '\u00A7');
			IItem item = classProvider.createItem();
			final IItem itemInstance = item;
			String[] args = null;

			for (int mode = 0, modeSize = Math.min(12, itemArguments.length() - 2); mode <= modeSize; ++mode)
			{
				args = itemArguments.substring(mode).split(Pattern.quote(" "));
				final IResourceLocation resourcelocation = classProvider.createResourceLocation(args[0]);
				item = functions.getObjectFromItemRegistry(resourcelocation);

				if (item != null)
					break;
			}

			if (item == null)
				return null;

			int i = 1;
			if (args.length >= 2 && PATTERN.matcher(args[1]).matches())
				i = Integer.parseInt(args[1]);

			int j = 0;
			if (args.length >= 3 && PATTERN.matcher(args[2]).matches())
				j = Integer.parseInt(args[2]);

			final IItemStack itemstack = classProvider.createItemStack(item, i, j);

			if (args.length >= 4)
			{
				final StringBuilder NBT = new StringBuilder();
				final int argsLength = args.length;
				for (int nbtcount = 3; nbtcount < argsLength; ++nbtcount)
					NBT.append(" ").append(args[nbtcount]);
				itemstack.setTagCompound(classProvider.getJsonToNBTInstance().getTagFromJson(NBT.toString()));
			}

			return itemstack;
		}
		catch (final Exception e)
		{
			// noinspection StringConcatenationArgumentToLogCall
			ClientUtils.getLogger().error("Can't create the item with arguments \"" + itemArguments + "\"", e);
			return null;
		}
	}

	public static int getEnchantment(final IItemStack itemStack, final IEnchantment enchantment)
	{
		return itemStack == null || itemStack.getEnchantmentTagList() == null || itemStack.getEnchantmentTagList().hasNoTags() ? 0 : IntStream.range(0, itemStack.getEnchantmentTagList().tagCount()).mapToObj(i -> itemStack.getEnchantmentTagList().getCompoundTagAt(i)).filter(tagCompound -> tagCompound.hasKey("ench") && tagCompound.getShort("ench") == enchantment.getEffectId() || tagCompound.hasKey("id") && tagCompound.getShort("id") == enchantment.getEffectId()).findFirst().map(tagCompound -> tagCompound.getShort("lvl")).orElseGet(() -> (short) 0);
	}

	public static int getEnchantmentCount(final IItemStack itemStack)
	{
		return itemStack == null || itemStack.getEnchantmentTagList() == null || itemStack.getEnchantmentTagList().hasNoTags() ? 0 : (int) IntStream.range(0, itemStack.getEnchantmentTagList().tagCount()).mapToObj(i -> itemStack.getEnchantmentTagList().getCompoundTagAt(i)).filter(tagCompound -> tagCompound.hasKey("ench") || tagCompound.hasKey("id")).count();
	}

	@Contract("null -> true")
	public static boolean isStackEmpty(final IItemStack stack)
	{
		return stack == null || classProvider.isItemAir(stack.getItem());
	}

	private static final Pattern PATTERN = Pattern.compile("\\d+"); // TODO: Rename
}
