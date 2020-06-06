/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

@ModuleInfo(name = "TNTBlock", description = "Automatically blocks with your sword when TNT around you explodes.", category = ModuleCategory.COMBAT)
public class TNTBlock extends Module {

    private final IntegerValue fuseValue = new IntegerValue("Fuse", 10, 0, 80);
    private final FloatValue rangeValue = new FloatValue("Range", 9, 1, 20);
    private final BoolValue autoSwordValue = new BoolValue("AutoSword", true);

    private boolean blocked;

    @EventTarget
    public void onMotionUpdate(MotionEvent event) {
        for(final Entity entity : mc.theWorld.loadedEntityList) {
            if(entity instanceof EntityTNTPrimed && mc.thePlayer.getDistanceToEntity(entity) <= rangeValue.get()) {
                final EntityTNTPrimed tntPrimed = (EntityTNTPrimed) entity;

                if(tntPrimed.fuse <= fuseValue.get()) {
                    if(autoSwordValue.get()) {
                        int slot = -1;
                        float bestDamage = 1F;

                        for(int i = 0; i < 9; i++) {
                            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

                            if(itemStack != null && itemStack.getItem() instanceof ItemSword) {
                                final float itemDamage = ((ItemSword) itemStack.getItem()).getDamageVsEntity() + 4F;

                                if(itemDamage > bestDamage) {
                                    bestDamage = itemDamage;
                                    slot = i;
                                }
                            }
                        }

                        if(slot != -1 && slot != mc.thePlayer.inventory.currentItem) {
                            mc.thePlayer.inventory.currentItem = slot;
                            mc.playerController.updateController();
                        }
                    }

                    if(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                        mc.gameSettings.keyBindUseItem.pressed = true;
                        blocked = true;
                    }
                    return;
                }
            }
        }

        if(blocked && !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
            mc.gameSettings.keyBindUseItem.pressed = false;
            blocked = false;
        }
    }
}