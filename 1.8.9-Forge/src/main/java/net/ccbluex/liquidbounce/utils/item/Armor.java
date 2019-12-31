package net.ccbluex.liquidbounce.utils.item;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public final class Armor {

    private static final int[] HELMET = new int[] {
            Item.getIdFromItem(Items.diamond_helmet),
            Item.getIdFromItem(Items.iron_helmet),
            Item.getIdFromItem(Items.chainmail_helmet),
            Item.getIdFromItem(Items.golden_helmet),
            Item.getIdFromItem(Items.leather_helmet)
    };

    private static final int[] CHESTPLATE = new int[] {
            Item.getIdFromItem(Items.diamond_chestplate),
            Item.getIdFromItem(Items.iron_chestplate),
            Item.getIdFromItem(Items.chainmail_chestplate),
            Item.getIdFromItem(Items.golden_chestplate),
            Item.getIdFromItem(Items.leather_chestplate)
    };

    private static final int[] LEGGINGS = new int[] {
            Item.getIdFromItem(Items.diamond_leggings),
            Item.getIdFromItem(Items.iron_leggings),
            Item.getIdFromItem(Items.chainmail_leggings),
            Item.getIdFromItem(Items.golden_leggings),
            Item.getIdFromItem(Items.leather_leggings)
    };

    private static final int[] BOOTS = new int[] {
            Item.getIdFromItem(Items.diamond_boots),
            Item.getIdFromItem(Items.iron_boots),
            Item.getIdFromItem(Items.chainmail_boots),
            Item.getIdFromItem(Items.golden_boots),
            Item.getIdFromItem(Items.leather_boots)
    };

    public static int[] getArmorArray(final int type) {
        switch(type) {
            case 0:
                return BOOTS;
            case 1:
                return LEGGINGS;
            case 2:
                return CHESTPLATE;
            case 3:
                return HELMET;
        }

        return null;
    }

    public static int[] getArmorArray(final ItemArmor itemArmor) {
        switch(itemArmor.armorType) {
            case 3:
                return BOOTS;
            case 2:
                return LEGGINGS;
            case 1:
                return CHESTPLATE;
            case 0:
                return HELMET;
        }

        return null;
    }

    public static int getArmorSlot(final int type) {
        switch(type) {
            case 0:
                return 8;
            case 1:
                return 7;
            case 2:
                return 6;
            case 3:
                return 5;
        }

        return -1;
    }
}