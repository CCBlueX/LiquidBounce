/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item;

import java.util.Objects;
import java.util.regex.Pattern;

import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment;
import net.ccbluex.liquidbounce.api.minecraft.item.IItem;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound;
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation;
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
			int i = 1;
			int j = 0;

			for (int mode = 0; mode <= Math.min(12, itemArguments.length() - 2); ++mode)
			{
				args = itemArguments.substring(mode).split(Pattern.quote(" "));
				final IResourceLocation resourcelocation = classProvider.createResourceLocation(args[0]);
				item = functions.getObjectFromItemRegistry(resourcelocation);

				if (item != null)
					break;
			}

			if (item == null)
				return null;

			if (Objects.requireNonNull(args).length >= 2 && args[1].matches("\\d+"))
				i = Integer.parseInt(args[1]);
			if (args.length >= 3 && args[2].matches("\\d+"))
				j = Integer.parseInt(args[2]);

			final IItemStack itemstack = classProvider.createItemStack(item, i, j);

			if (args.length >= 4)
			{
				final StringBuilder NBT = new StringBuilder();
				for (int nbtcount = 3; nbtcount < args.length; ++nbtcount)
					NBT.append(" ").append(args[nbtcount]);
				itemstack.setTagCompound(classProvider.getJsonToNBTInstance().getTagFromJson(NBT.toString()));
			}

			return itemstack;
		}
		catch (final Exception exception)
		{
			exception.printStackTrace();
			return null;
		}
	}

	public static int getEnchantment(final IItemStack itemStack, final IEnchantment enchantment)
	{
		if (itemStack == null || itemStack.getEnchantmentTagList() == null || itemStack.getEnchantmentTagList().hasNoTags())
			return 0;

		for (int i = 0; i < itemStack.getEnchantmentTagList().tagCount(); i++)
		{
			final INBTTagCompound tagCompound = itemStack.getEnchantmentTagList().getCompoundTagAt(i);

			if (tagCompound.hasKey("ench") && tagCompound.getShort("ench") == enchantment.getEffectId() || tagCompound.hasKey("id") && tagCompound.getShort("id") == enchantment.getEffectId())
				return tagCompound.getShort("lvl");
		}

		return 0;
	}

	public static int getEnchantmentCount(final IItemStack itemStack)
	{
		if (itemStack == null || itemStack.getEnchantmentTagList() == null || itemStack.getEnchantmentTagList().hasNoTags())
			return 0;

		int c = 0;

		for (int i = 0; i < itemStack.getEnchantmentTagList().tagCount(); i++)
		{
			final INBTTagCompound tagCompound = itemStack.getEnchantmentTagList().getCompoundTagAt(i);

			if (tagCompound.hasKey("ench") || tagCompound.hasKey("id"))
				c++;
		}

		return c;
	}

	@Contract("null -> true")
	public static boolean isStackEmpty(final IItemStack stack)
	{
		return stack == null || classProvider.isItemAir(stack.getItem());
	}
}
