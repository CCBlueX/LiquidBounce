package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.injection.implementations.IItemStack;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.item.Armor;
import net.ccbluex.liquidbounce.utils.item.ItemUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "AutoRüstung", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.COMBAT)
public class AutoArmor extends Module {

    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 200, 0, 400) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int minDelay = minDelayValue.get();

            if(minDelay > newValue) set(minDelay);
        }
    };

    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 100, 0, 400) {

        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxDelayValue.get();

            if(maxDelay < newValue) set(maxDelay);
        }
    };

    private final BoolValue invOpenValue = new BoolValue("InvOpen", false);
    private final BoolValue simulateInventory = new BoolValue("SimulateInventory", true);
    private final BoolValue noMoveValue = new BoolValue("NoMove", false);
    private final IntegerValue itemDelayValue = new IntegerValue("ItemDelay", 0, 0, 5000);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);

    private final MSTimer msTimer = new MSTimer();
    private long delay;

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if(!msTimer.hasTimePassed(delay) || (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0))
            return;

        int item = -1;
        int hotbarItem = -1;

        for(int slot = 0; slot <= 3; slot++) {
            final int[] idArray = Armor.getArmorArray(slot);

            if(mc.thePlayer.inventory.armorInventory[slot] == null) {
                item = getArmor(idArray, 9, 45, false);
                hotbarItem = getArmor(idArray, 36, 45, true);
            }else if(hasBetter(slot, idArray))
                item = Armor.getArmorSlot(slot);

            if(item != -1)
                break;
        }

        if(!(noMoveValue.get() && MovementUtils.isMoving()) && (!invOpenValue.get() || mc.currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(mc.currentScreen instanceof GuiInventory);

            if(openInventory)
                mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));

            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, item, 0, 1, mc.thePlayer);

            msTimer.reset();
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if(openInventory)
                mc.getNetHandler().addToSendQueue(new C0DPacketCloseWindow());
        }else if(hotbarValue.get() && hotbarItem != -1) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(hotbarItem - 36));
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(hotbarItem).getStack()));
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));

            msTimer.reset();
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
        }
    }

    private boolean hasBetter(final int slot, final int[] type) {
        int armorIndex = -1;
        int inventoryIndex = -1;
        int inventorySlot = -1;

        for(int i = 0; i < type.length; i++) {
            if(mc.thePlayer.inventory.armorInventory[slot] != null && Item.getIdFromItem(mc.thePlayer.inventory.armorInventory[slot].getItem()) == type[i]) {
                armorIndex = i;
                break;
            }
        }

        for(int i = 0; i < type.length; i++) {
            if((inventorySlot = getArmorItem(type[i], 9, 45, false)) != -1) {
                inventoryIndex = i;
                break;
            }
        }

        if(inventoryIndex <= -1)
            return false;

        return inventoryIndex < armorIndex || (inventoryIndex == armorIndex && ItemUtils.getEnchantment(mc.thePlayer.inventory.armorInventory[slot], Enchantment.protection) < ItemUtils.getEnchantment(mc.thePlayer.inventoryContainer.getSlot(inventorySlot).getStack(), Enchantment.protection));
    }

    private int getArmor(final int[] ids, final int startSlot, final int endSlot, final boolean hotbar) {
        for(final int id : ids) {
            final int i = getArmorItem(id, startSlot, endSlot, hotbar);

            if(i != -1)
                return i;
        }

        return -1;
    }

    private int getArmorItem(final int id, final int startSlot, final int endSlot, boolean hotbar) {
        int bestSlot = -1;

        for(int index = startSlot; index < endSlot; index++) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(index).getStack();

            if(itemStack == null || (!hotbar && System.currentTimeMillis() - ((IItemStack) (Object) itemStack).getItemDelay() < itemDelayValue.get()))
                continue;

            if(Item.getIdFromItem(itemStack.getItem()) == id && (bestSlot == -1 || ItemUtils.getEnchantment(itemStack, Enchantment.protection) >= ItemUtils.getEnchantment(mc.thePlayer.inventoryContainer.getSlot(bestSlot).getStack(), Enchantment.protection)))
                bestSlot = index;
        }

        return bestSlot;
    }
}
