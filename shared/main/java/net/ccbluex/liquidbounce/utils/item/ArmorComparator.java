/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item;

import static net.ccbluex.liquidbounce.utils.item.ItemUtils.getEnchantmentCount;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

import net.ccbluex.liquidbounce.api.enums.EnchantmentType;
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemArmor;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;

public class ArmorComparator extends MinecraftInstance implements Comparator<ArmorPiece>, Serializable
{
	private static final IEnchantment[] DAMAGE_REDUCTION_ENCHANTMENTS =
	{
			classProvider.getEnchantmentEnum(EnchantmentType.PROTECTION),
			classProvider.getEnchantmentEnum(EnchantmentType.PROJECTILE_PROTECTION),
			classProvider.getEnchantmentEnum(EnchantmentType.FIRE_PROTECTION),
			classProvider.getEnchantmentEnum(EnchantmentType.BLAST_PROTECTION)
	};

	private static final float[] ENCHANTMENT_FACTORS =
	{
			1.5f, // PROTECTION
			0.4f, // PROJECTILE_PROTECTION
			0.39f, // FIRE_PROTECTION
			0.38f // BLAST_PROTECTION
	};
	private static final float[] ENCHANTMENT_DAMAGE_REDUCTION_FACTOR =
	{
			0.04f, // PROTECTION
			0.08f, // PROJECTILE_PROTECTION
			0.15f, // FIRE_PROTECTION
			0.08f // BLAST_PROTECTION
	};

	private static final IEnchantment[] OTHER_ENCHANTMENTS =
	{
			classProvider.getEnchantmentEnum(EnchantmentType.FEATHER_FALLING),
			classProvider.getEnchantmentEnum(EnchantmentType.THORNS),
			classProvider.getEnchantmentEnum(EnchantmentType.RESPIRATION),
			classProvider.getEnchantmentEnum(EnchantmentType.AQUA_AFFINITY),
			classProvider.getEnchantmentEnum(EnchantmentType.UNBREAKING)
	};
	private static final float[] OTHER_ENCHANTMENT_FACTORS =
	{
			3.0f, // FEATHER_FALLING
			1.0f, // THORNS
			0.1f, // RESPIRATION
			0.05f, // AQUA_AFFINITY
			0.01f // UNBREAKING
	};

	/**
	 * Rounds a double. From https://stackoverflow.com/a/2808648/9140494
	 *
	 * @param  value
	 *                the value to be rounded
	 * @param  places
	 *                Decimal places
	 * @return        The rounded value
	 */
	public static double round(final double value, final int places)
	{
		if (places < 0)
			throw new IllegalArgumentException("places");

		final BigDecimal bd = BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	@Override
	public int compare(final ArmorPiece o1, final ArmorPiece o2)
	{
		// For damage reduction it is better if it is smaller, so it has to be inverted
		// The decimal values have to be rounded since in double math equals is inaccurate
		// For example 1.03 - 0.41 = 0.6200000000000001 and (1.03 - 0.41) == 0.62 would be false
		final int compare = Double.compare(round(getThresholdedDamageReduction(o2.getItemStack()), 3), round(getThresholdedDamageReduction(o1.getItemStack()), 3));

		// If both armor pieces have the exact same damage, compare enchantments
		if (compare == 0)
		{
			final int otherEnchantmentCmp = Double.compare(round(getEnchantmentThreshold(o1.getItemStack()), 3), round(getEnchantmentThreshold(o2.getItemStack()), 3));

			// If both have the same enchantment threshold, prefer the item with more enchantments
			if (otherEnchantmentCmp == 0)
			{
				final int enchantmentCountCmp = Integer.compare(getEnchantmentCount(o1.getItemStack()), getEnchantmentCount(o2.getItemStack()));

				if (enchantmentCountCmp != 0)
					return enchantmentCountCmp;

				// Then durability...
				final IItemArmor o1a = o1.getItemStack().getItem().asItemArmor();
				final IItemArmor o2a = o2.getItemStack().getItem().asItemArmor();

				final int durabilityCmp = Integer.compare(o1a.getArmorMaterial().getDurability(o1a.getArmorType()), o2a.getArmorMaterial().getDurability(o2a.getArmorType()));

				// Last comparision: Enchantability
				return durabilityCmp != 0 ? durabilityCmp : Integer.compare(o1a.getArmorMaterial().getEnchantability(), o2a.getArmorMaterial().getEnchantability());
			}

			return otherEnchantmentCmp;
		}

		return compare;
	}

	private float getThresholdedDamageReduction(final IItemStack itemStack)
	{
		final IItemArmor item = itemStack.getItem().asItemArmor();

		return getDamageReduction(item.getArmorMaterial().getDamageReductionAmount(item.getArmorType()), 0) * (1 - getThresholdedEnchantmentDamageReduction(itemStack));
	}

	private float getDamageReduction(final int defensePoints, final int toughness)
	{
		return 1 - Math.min(20.0f, Math.max(defensePoints / 5.0f, defensePoints - 1 / (2 + toughness / 4.0f))) / 25.0f;
	}

	private static float getThresholdedEnchantmentDamageReduction(final IItemStack itemStack)
	{
		float sum = 0.0f;

		for (int i = 0, j = DAMAGE_REDUCTION_ENCHANTMENTS.length; i < j; i++)
			sum += ItemUtils.getEnchantment(itemStack, DAMAGE_REDUCTION_ENCHANTMENTS[i]) * ENCHANTMENT_FACTORS[i] * ENCHANTMENT_DAMAGE_REDUCTION_FACTOR[i];

		return sum;

	}

	private static float getEnchantmentThreshold(final IItemStack itemStack)
	{
		float sum = 0.0f;

		for (int i = 0, j = OTHER_ENCHANTMENTS.length; i < j; i++)
			sum += ItemUtils.getEnchantment(itemStack, OTHER_ENCHANTMENTS[i]) * OTHER_ENCHANTMENT_FACTORS[i];

		return sum;

	}
}
