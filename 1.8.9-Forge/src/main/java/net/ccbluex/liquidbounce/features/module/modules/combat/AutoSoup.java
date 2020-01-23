/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@ModuleInfo(name = "AutoSoup", description = "Makes you automatically eat soup whenever your health is low.", category = ModuleCategory.COMBAT)
public class AutoSoup extends Module {
    private final FloatValue healthValue = new FloatValue("Health", 15F, 0F, 20F);
    private final IntegerValue delayValue = new IntegerValue("Delay", 150, 0, 500);
    private final BoolValue openInventoryValue = new BoolValue("OpenInv", false);

    private final MSTimer msTimer = new MSTimer();

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if(!msTimer.hasTimePassed(delayValue.get()))
            return;

        final int soupInHotbar = InventoryUtils.findItem(36, 45, Items.mushroom_stew);
        if(mc.thePlayer.getHealth() <= healthValue.get() && soupInHotbar != -1) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(soupInHotbar - 36));
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(soupInHotbar).getStack()));
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            msTimer.reset();
            return;
        }

        final int soupInInventory = InventoryUtils.findItem(9, 36, Items.mushroom_stew);
        if(soupInInventory != -1 && InventoryUtils.hasSpaceHotbar()) {
            if(openInventoryValue.get() && !(mc.currentScreen instanceof GuiInventory))
                return;

            mc.playerController.windowClick(0, soupInInventory, 0, 1, mc.thePlayer);
            msTimer.reset();
        }
    }

    @Override
    public String getTag() {
        return String.valueOf(healthValue.get());
    }
}
