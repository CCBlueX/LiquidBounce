package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "AutoTrank", description = "Automatically throws healing potions.", category = ModuleCategory.COMBAT)
public class AutoPot extends Module {

    private final MSTimer msTimer = new MSTimer();

    private final FloatValue healthValue = new FloatValue("Health", 15F, 1F, 20F);
    private final IntegerValue delayValue = new IntegerValue("Delay", 150, 0, 500);
    private final BoolValue openInventoryValue = new BoolValue("OpenInv", false);
    private final BoolValue noAirValue = new BoolValue("NoAir", false);
    private final ListValue modeValue = new ListValue("Mode", new String[] {"Normal", "Jump", "Port"}, "Normal");

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if(!msTimer.hasTimePassed(delayValue.get()) || mc.playerController.isInCreativeMode())
            return;

        if (noAirValue.get() && !mc.thePlayer.onGround)
            return;

        // Hotbar Potion
        final int potionInHotbar = findPotion(36, 45);

        if(mc.thePlayer.getHealth() <= healthValue.get() && potionInHotbar != -1) {
            if(mc.thePlayer.onGround) {
                switch(modeValue.get().toLowerCase()) {
                    case "jump":
                        mc.thePlayer.jump();
                        break;
                    case "port":
                        mc.thePlayer.moveEntity(0, 0.42D, 0);
                        break;
                }
            }

            throwPot(potionInHotbar);
            msTimer.reset();
            return;
        }

        // Inventory Potion -> Hotbar Potion
        final int potionInInventory = findPotion(9, 36);

        if(potionInInventory != -1 && InventoryUtils.hasSpaceHotbar()) {
            if(openInventoryValue.get() && !(mc.currentScreen instanceof GuiInventory))
                return;

            mc.playerController.windowClick(0, potionInInventory, 0, 1, mc.thePlayer);
            msTimer.reset();
        }
    }

    private void throwPot(final int slot) {
        RotationUtils.keepCurrentRotation = true;
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, 90F, mc.thePlayer.onGround));
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(slot - 36));
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(slot).getStack()));
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        RotationUtils.keepCurrentRotation = false;
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
    }

    private int findPotion(final int startSlot, final int endSlot) {
        for(int i = startSlot; i < endSlot; i++) {
            final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if(stack == null || !(stack.getItem() instanceof ItemPotion) || !ItemPotion.isSplash(stack.getItemDamage()))
                continue;

            final ItemPotion itemPotion = (ItemPotion) stack.getItem();

            for(final PotionEffect potionEffect : itemPotion.getEffects(stack))
                if(potionEffect.getPotionID() == Potion.heal.id)
                    return i;

            if(!mc.thePlayer.isPotionActive(Potion.regeneration))
                for(final PotionEffect potionEffect : itemPotion.getEffects(stack))
                    if(potionEffect.getPotionID() == Potion.regeneration.id)
                        return i;
        }
        return -1;
    }

    @Override
    public String getTag() {
        return String.valueOf(healthValue.get());
    }
}
