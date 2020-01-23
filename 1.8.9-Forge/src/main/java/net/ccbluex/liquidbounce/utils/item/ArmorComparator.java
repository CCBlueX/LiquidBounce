/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import java.util.Comparator;

public class ArmorComparator implements Comparator<ArmorPiece> {
    private static final Enchantment[] DAMAGE_REDUCTION_ENCHANTMENTS = {Enchantment.protection, Enchantment.projectileProtection, Enchantment.fireProtection, Enchantment.blastProtection};
    private static final float[] ENCHANTMENT_FACTORS = {1.5f, 0.4f, 0.39f, 0.38f};
    private static final float[] ENCHANTMENT_DAMAGE_REDUCTION_FACTOR = {0.04f, 0.08f, 0.15f, 0.08f};

    @Override
    public int compare(ArmorPiece o1, ArmorPiece o2) {
        // For damage reduction it is better if it is smaller, so it has to be inverted
        return Float.compare(getThresholdedDamageReduction(o2.getItemStack()), getThresholdedDamageReduction(o1.getItemStack()));
    }

    private float getThresholdedDamageReduction(ItemStack itemStack) {
        ItemArmor item = (ItemArmor) itemStack.getItem();

        return getDamageReduction(item.getArmorMaterial().getDamageReductionAmount(item.armorType), 0) * (1 - getThresholdedEnchantmentDamageReduction(itemStack));
    }

    private float getDamageReduction(int defensePoints, int toughness) {
        return 1 - Math.min(20.0f, Math.max(defensePoints / 5.0f, defensePoints - 1 / (2 + toughness / 4.0f))) / 25.0f;
    }

    private float getThresholdedEnchantmentDamageReduction(ItemStack itemStack) {
        float sum = 0.0f;

        for (int i = 0; i < DAMAGE_REDUCTION_ENCHANTMENTS.length; i++) {
            sum += ItemUtils.getEnchantment(itemStack, DAMAGE_REDUCTION_ENCHANTMENTS[i]) * ENCHANTMENT_FACTORS[i] * ENCHANTMENT_DAMAGE_REDUCTION_FACTOR[i];
        }

        return sum;

    }

}
