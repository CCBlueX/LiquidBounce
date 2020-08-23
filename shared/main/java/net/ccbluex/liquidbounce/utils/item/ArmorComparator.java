/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item;

import net.ccbluex.liquidbounce.api.enums.EnchantmentType;
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemArmor;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

import static net.ccbluex.liquidbounce.utils.item.ItemUtils.getEnchantmentCount;

public class ArmorComparator extends MinecraftInstance implements Comparator<ArmorPiece> {
    private static final IEnchantment[] DAMAGE_REDUCTION_ENCHANTMENTS = {classProvider.getEnchantmentEnum(EnchantmentType.PROTECTION), classProvider.getEnchantmentEnum(EnchantmentType.PROJECTILE_PROTECTION), classProvider.getEnchantmentEnum(EnchantmentType.FIRE_PROTECTION), classProvider.getEnchantmentEnum(EnchantmentType.BLAST_PROTECTION)};
    private static final float[] ENCHANTMENT_FACTORS = {1.5f, 0.4f, 0.39f, 0.38f};
    private static final float[] ENCHANTMENT_DAMAGE_REDUCTION_FACTOR = {0.04f, 0.08f, 0.15f, 0.08f};
    private static final IEnchantment[] OTHER_ENCHANTMENTS = {classProvider.getEnchantmentEnum(EnchantmentType.FEATHER_FALLING), classProvider.getEnchantmentEnum(EnchantmentType.THORNS), classProvider.getEnchantmentEnum(EnchantmentType.RESPIRATION), classProvider.getEnchantmentEnum(EnchantmentType.AQUA_AFFINITY), classProvider.getEnchantmentEnum(EnchantmentType.UNBREAKING)};
    private static final float[] OTHER_ENCHANTMENT_FACTORS = {3.0f, 1.0f, 0.1f, 0.05f, 0.01f};

    /**
     * Rounds a double. From https://stackoverflow.com/a/2808648/9140494
     *
     * @param value  the value to be rounded
     * @param places Decimal places
     * @return The rounded value
     */
    public static double round(double value, int places) {
        if (places < 0)
            throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public int compare(ArmorPiece o1, ArmorPiece o2) {
        // For damage reduction it is better if it is smaller, so it has to be inverted
        // The decimal values have to be rounded since in double math equals is inaccurate
        // For example 1.03 - 0.41 = 0.6200000000000001 and (1.03 - 0.41) == 0.62 would be false
        int compare = Double.compare(round(getThresholdedDamageReduction(o2.getItemStack()), 3), round(getThresholdedDamageReduction(o1.getItemStack()), 3));

        // If both armor pieces have the exact same damage, compare enchantments
        if (compare == 0) {
            int otherEnchantmentCmp = Double.compare(round(getEnchantmentThreshold(o1.getItemStack()), 3), round(getEnchantmentThreshold(o2.getItemStack()), 3));

            // If both have the same enchantment threshold, prefer the item with more enchantments
            if (otherEnchantmentCmp == 0) {
                int enchantmentCountCmp = Integer.compare(getEnchantmentCount(o1.getItemStack()), getEnchantmentCount(o2.getItemStack()));

                if (enchantmentCountCmp != 0)
                    return enchantmentCountCmp;

                // Then durability...
                IItemArmor o1a = (o1.getItemStack().getItem()).asItemArmor();
                IItemArmor o2a = (o2.getItemStack().getItem()).asItemArmor();

                int durabilityCmp = Integer.compare(o1a.getArmorMaterial().getDurability(o1a.getArmorType()), o2a.getArmorMaterial().getDurability(o2a.getArmorType()));

                if (durabilityCmp != 0) {
                    return durabilityCmp;
                }

                // Last comparision: Enchantability
                return Integer.compare(o1a.getArmorMaterial().getEnchantability(), o2a.getArmorMaterial().getEnchantability());
            }

            return otherEnchantmentCmp;
        }

        return compare;
    }

    private float getThresholdedDamageReduction(IItemStack itemStack) {
        IItemArmor item = itemStack.getItem().asItemArmor();

        return getDamageReduction(item.getArmorMaterial().getDamageReductionAmount(item.getArmorType()), 0) * (1 - getThresholdedEnchantmentDamageReduction(itemStack));
    }

    private float getDamageReduction(int defensePoints, int toughness) {
        return 1 - Math.min(20.0f, Math.max(defensePoints / 5.0f, defensePoints - 1 / (2 + toughness / 4.0f))) / 25.0f;
    }

    private float getThresholdedEnchantmentDamageReduction(IItemStack itemStack) {
        float sum = 0.0f;

        for (int i = 0; i < DAMAGE_REDUCTION_ENCHANTMENTS.length; i++) {
            sum += ItemUtils.getEnchantment(itemStack, DAMAGE_REDUCTION_ENCHANTMENTS[i]) * ENCHANTMENT_FACTORS[i] * ENCHANTMENT_DAMAGE_REDUCTION_FACTOR[i];
        }

        return sum;

    }

    private float getEnchantmentThreshold(IItemStack itemStack) {
        float sum = 0.0f;

        for (int i = 0; i < OTHER_ENCHANTMENTS.length; i++) {
            sum += ItemUtils.getEnchantment(itemStack, OTHER_ENCHANTMENTS[i]) * OTHER_ENCHANTMENT_FACTORS[i];
        }

        return sum;

    }

}
