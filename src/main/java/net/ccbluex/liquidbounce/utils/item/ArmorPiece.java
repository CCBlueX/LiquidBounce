/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item;


import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;

public class ArmorPiece {
    private final IItemStack itemStack;
    private final int slot;

    public ArmorPiece(IItemStack itemStack, int slot) {
        this.itemStack = itemStack;
        this.slot = slot;
    }

    public int getArmorType() {
        return itemStack.getItem().asItemArmor().getArmorType();
    }

    public int getSlot() {
        return slot;
    }

    public IItemStack getItemStack() {
        return itemStack;
    }
}
